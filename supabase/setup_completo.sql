-- ============================================================================
-- ESUCAR · Esquema Supabase (Postgres) para la Evaluación 3
-- Ejecutar completo en: Supabase Dashboard > SQL Editor > New query
-- Es idempotente: se puede volver a ejecutar sin borrar datos.
-- ============================================================================

create extension if not exists pgcrypto;

-- ---------------------------------------------------------------------------
-- Tipos
-- ---------------------------------------------------------------------------
do $$
begin
    if not exists (select 1 from pg_type where typname = 'user_role') then
        create type public.user_role as enum ('ESTUDIANTE', 'DOCENTE');
    end if;
    if not exists (select 1 from pg_type where typname = 'material_type') then
        create type public.material_type as enum ('PDF', 'VIDEO', 'PRESENTACION', 'GUIA');
    end if;
end $$;

-- ---------------------------------------------------------------------------
-- Configuración de la aplicación
-- El código de invitación evita que cualquiera se registre como docente.
-- Cámbialo antes de la entrega y compártelo solo con el docente real.
-- ---------------------------------------------------------------------------
create table if not exists public.app_settings (
    key   text primary key,
    value text not null
);

insert into public.app_settings (key, value)
values ('teacher_invite_code', 'ESUCAR-DOCENTE-2026')
on conflict (key) do nothing;

-- ---------------------------------------------------------------------------
-- Perfiles (1:1 con auth.users)
-- ---------------------------------------------------------------------------
create table if not exists public.profiles (
    id         uuid primary key references auth.users (id) on delete cascade,
    full_name  text not null,
    email      text not null,
    role       public.user_role not null default 'ESTUDIANTE',
    created_at timestamptz not null default now()
);

create index if not exists profiles_role_idx on public.profiles (role);

-- ---------------------------------------------------------------------------
-- Asignaturas / unidades / materiales
-- ---------------------------------------------------------------------------
create table if not exists public.courses (
    id          uuid primary key default gen_random_uuid(),
    code        text not null unique,
    name        text not null,
    description text not null default '',
    color       text not null default '#15803d',
    teacher_id  uuid references public.profiles (id) on delete set null,
    position    int  not null default 0,
    created_at  timestamptz not null default now()
);

create table if not exists public.units (
    id         uuid primary key default gen_random_uuid(),
    course_id  uuid not null references public.courses (id) on delete cascade,
    number     int  not null,
    title      text not null,
    position   int  not null default 0,
    created_at timestamptz not null default now(),
    unique (course_id, number)
);

create index if not exists units_course_idx on public.units (course_id);

create table if not exists public.materials (
    id           uuid primary key default gen_random_uuid(),
    unit_id      uuid not null references public.units (id) on delete cascade,
    title        text not null,
    type         public.material_type not null default 'PDF',
    description  text not null default '',
    storage_path text,
    file_name    text,
    mime_type    text,
    size_bytes   bigint,
    uploaded_by  uuid references public.profiles (id) on delete set null,
    position     int  not null default 0,
    created_at   timestamptz not null default now(),
    updated_at   timestamptz not null default now()
);

create index if not exists materials_unit_idx on public.materials (unit_id);

-- ---------------------------------------------------------------------------
-- Mensajería docente <-> estudiante
-- ---------------------------------------------------------------------------
create table if not exists public.messages (
    id          uuid primary key default gen_random_uuid(),
    student_id  uuid not null references public.profiles (id) on delete cascade,
    student_name text not null default '',
    teacher_id  uuid references public.profiles (id) on delete set null,
    course_id   uuid not null references public.courses (id) on delete cascade,
    unit_id     uuid references public.units (id) on delete set null,
    material_id uuid references public.materials (id) on delete set null,
    subject     text not null,
    content     text not null,
    read_at     timestamptz,
    created_at  timestamptz not null default now()
);

create index if not exists messages_student_idx on public.messages (student_id);
create index if not exists messages_created_idx on public.messages (created_at desc);

create table if not exists public.replies (
    id         uuid primary key default gen_random_uuid(),
    message_id uuid not null references public.messages (id) on delete cascade,
    teacher_id uuid not null references public.profiles (id) on delete cascade,
    content    text not null,
    created_at timestamptz not null default now()
);

create index if not exists replies_message_idx on public.replies (message_id);

-- ---------------------------------------------------------------------------
-- Progreso y descargas POR USUARIO (esto corrige el flag global de la app)
-- ---------------------------------------------------------------------------
create table if not exists public.progress (
    student_id  uuid not null references public.profiles (id) on delete cascade,
    material_id uuid not null references public.materials (id) on delete cascade,
    studied_at  timestamptz not null default now(),
    primary key (student_id, material_id)
);

create table if not exists public.downloads (
    student_id    uuid not null references public.profiles (id) on delete cascade,
    material_id   uuid not null references public.materials (id) on delete cascade,
    downloaded_at timestamptz not null default now(),
    primary key (student_id, material_id)
);

-- ---------------------------------------------------------------------------
-- Notificaciones por usuario (no por rol)
-- ---------------------------------------------------------------------------
create table if not exists public.notifications (
    id         uuid primary key default gen_random_uuid(),
    user_id    uuid not null references public.profiles (id) on delete cascade,
    type       text not null check (type in ('CONSULTA', 'RESPUESTA', 'MATERIAL')),
    title      text not null,
    body       text not null default '',
    related_id uuid,
    read_at    timestamptz,
    created_at timestamptz not null default now()
);

create index if not exists notifications_user_idx on public.notifications (user_id, created_at desc);

-- ---------------------------------------------------------------------------
-- Helpers de autorización
-- SECURITY DEFINER evita recursión en las políticas RLS de profiles.
-- ---------------------------------------------------------------------------
create or replace function public.my_role()
returns text
language sql
stable
security definer
set search_path = public
as $$
    select role::text from public.profiles where id = auth.uid()
$$;

create or replace function public.is_teacher()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
    select coalesce(
        (select role = 'DOCENTE' from public.profiles where id = auth.uid()),
        false
    )
$$;

-- ---------------------------------------------------------------------------
-- Perfil automático al registrarse en Supabase Auth
-- El rol DOCENTE exige el código de invitación; si no coincide, queda ESTUDIANTE.
-- ---------------------------------------------------------------------------
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    requested text;
    provided  text;
    expected  text;
    final_role public.user_role;
begin
    requested := upper(coalesce(new.raw_user_meta_data ->> 'role', 'ESTUDIANTE'));
    provided  := coalesce(new.raw_user_meta_data ->> 'teacher_code', '');
    select value into expected from public.app_settings where key = 'teacher_invite_code';

    if requested = 'DOCENTE' and expected is not null and provided = expected then
        final_role := 'DOCENTE';
    else
        final_role := 'ESTUDIANTE';
    end if;

    insert into public.profiles (id, full_name, email, role)
    values (
        new.id,
        coalesce(nullif(trim(new.raw_user_meta_data ->> 'full_name'), ''), split_part(new.email, '@', 1)),
        new.email,
        final_role
    )
    on conflict (id) do nothing;

    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- Mantiene updated_at de materiales
create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at := now();
    return new;
end;
$$;

drop trigger if exists materials_touch_updated_at on public.materials;
create trigger materials_touch_updated_at
    before update on public.materials
    for each row execute function public.touch_updated_at();

-- ---------------------------------------------------------------------------
-- Notificaciones automáticas
-- Se generan en el servidor: el cliente no puede inventar avisos para otros.
-- ---------------------------------------------------------------------------
create or replace function public.notify_new_message()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    teacher uuid;
begin
    select c.teacher_id into teacher
    from public.courses c
    where c.id = new.course_id;

    if teacher is not null then
        insert into public.notifications (user_id, type, title, body, related_id)
        values (teacher, 'CONSULTA', 'Nueva consulta de ' || new.student_name, new.subject, new.id);
    else
        insert into public.notifications (user_id, type, title, body, related_id)
        select p.id, 'CONSULTA', 'Nueva consulta de ' || new.student_name, new.subject, new.id
        from public.profiles p
        where p.role = 'DOCENTE';
    end if;
    return new;
end;
$$;

drop trigger if exists messages_notify_teacher on public.messages;
create trigger messages_notify_teacher
    after insert on public.messages
    for each row execute function public.notify_new_message();

create or replace function public.notify_new_reply()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    owner uuid;
    subject text;
begin
    select m.student_id, m.subject into owner, subject
    from public.messages m
    where m.id = new.message_id;

    if owner is not null then
        insert into public.notifications (user_id, type, title, body, related_id)
        values (owner, 'RESPUESTA', 'Respondieron tu consulta', subject, new.message_id);
    end if;
    return new;
end;
$$;

drop trigger if exists replies_notify_student on public.replies;
create trigger replies_notify_student
    after insert on public.replies
    for each row execute function public.notify_new_reply();

create or replace function public.notify_new_material()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.notifications (user_id, type, title, body, related_id)
    select p.id, 'MATERIAL', 'Nuevo material disponible', new.title, new.id
    from public.profiles p
    where p.role = 'ESTUDIANTE';
    return new;
end;
$$;

drop trigger if exists materials_notify_students on public.materials;
create trigger materials_notify_students
    after insert on public.materials
    for each row execute function public.notify_new_material();

-- ---------------------------------------------------------------------------
-- RLS
-- ---------------------------------------------------------------------------
alter table public.profiles      enable row level security;
alter table public.courses       enable row level security;
alter table public.units         enable row level security;
alter table public.materials     enable row level security;
alter table public.messages      enable row level security;
alter table public.replies       enable row level security;
alter table public.progress      enable row level security;
alter table public.downloads     enable row level security;
alter table public.notifications enable row level security;

-- profiles -------------------------------------------------------------------
drop policy if exists "profiles_select" on public.profiles;
create policy "profiles_select" on public.profiles
    for select to authenticated
    using (id = auth.uid() or public.is_teacher());

drop policy if exists "profiles_update_own" on public.profiles;
create policy "profiles_update_own" on public.profiles
    for update to authenticated
    using (id = auth.uid())
    with check (id = auth.uid() and role = public.my_role()::public.user_role);

-- courses --------------------------------------------------------------------
drop policy if exists "courses_select" on public.courses;
create policy "courses_select" on public.courses
    for select to authenticated using (true);

drop policy if exists "courses_write" on public.courses;
create policy "courses_write" on public.courses
    for all to authenticated
    using (public.is_teacher())
    with check (public.is_teacher());

-- units ----------------------------------------------------------------------
drop policy if exists "units_select" on public.units;
create policy "units_select" on public.units
    for select to authenticated using (true);

drop policy if exists "units_write" on public.units;
create policy "units_write" on public.units
    for all to authenticated
    using (public.is_teacher())
    with check (public.is_teacher());

-- materials ------------------------------------------------------------------
drop policy if exists "materials_select" on public.materials;
create policy "materials_select" on public.materials
    for select to authenticated using (true);

drop policy if exists "materials_write" on public.materials;
create policy "materials_write" on public.materials
    for all to authenticated
    using (public.is_teacher())
    with check (public.is_teacher());

-- messages -------------------------------------------------------------------
drop policy if exists "messages_select" on public.messages;
create policy "messages_select" on public.messages
    for select to authenticated
    using (student_id = auth.uid() or public.is_teacher());

drop policy if exists "messages_insert_student" on public.messages;
create policy "messages_insert_student" on public.messages
    for insert to authenticated
    with check (student_id = auth.uid() and not public.is_teacher());

drop policy if exists "messages_update" on public.messages;
create policy "messages_update" on public.messages
    for update to authenticated
    using (student_id = auth.uid() or public.is_teacher())
    with check (student_id = auth.uid() or public.is_teacher());

drop policy if exists "messages_delete_own" on public.messages;
create policy "messages_delete_own" on public.messages
    for delete to authenticated
    using (student_id = auth.uid() or public.is_teacher());

-- replies --------------------------------------------------------------------
drop policy if exists "replies_select" on public.replies;
create policy "replies_select" on public.replies
    for select to authenticated
    using (
        public.is_teacher()
        or exists (
            select 1 from public.messages m
            where m.id = replies.message_id and m.student_id = auth.uid()
        )
    );

drop policy if exists "replies_insert_teacher" on public.replies;
create policy "replies_insert_teacher" on public.replies
    for insert to authenticated
    with check (public.is_teacher() and teacher_id = auth.uid());

-- progress -------------------------------------------------------------------
drop policy if exists "progress_select_own" on public.progress;
create policy "progress_select_own" on public.progress
    for select to authenticated
    using (student_id = auth.uid() or public.is_teacher());

drop policy if exists "progress_write_own" on public.progress;
create policy "progress_write_own" on public.progress
    for all to authenticated
    using (student_id = auth.uid())
    with check (student_id = auth.uid());

-- downloads ------------------------------------------------------------------
drop policy if exists "downloads_own" on public.downloads;
create policy "downloads_own" on public.downloads
    for all to authenticated
    using (student_id = auth.uid())
    with check (student_id = auth.uid());

-- notifications --------------------------------------------------------------
drop policy if exists "notifications_select_own" on public.notifications;
create policy "notifications_select_own" on public.notifications
    for select to authenticated
    using (user_id = auth.uid());

drop policy if exists "notifications_update_own" on public.notifications;
create policy "notifications_update_own" on public.notifications
    for update to authenticated
    using (user_id = auth.uid())
    with check (user_id = auth.uid());

-- ---------------------------------------------------------------------------
-- Storage: bucket privado para los archivos de material
-- ---------------------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('materials', 'materials', false)
on conflict (id) do nothing;

drop policy if exists "materials_storage_read" on storage.objects;
create policy "materials_storage_read" on storage.objects
    for select to authenticated
    using (bucket_id = 'materials');

drop policy if exists "materials_storage_insert" on storage.objects;
create policy "materials_storage_insert" on storage.objects
    for insert to authenticated
    with check (bucket_id = 'materials' and public.is_teacher());

drop policy if exists "materials_storage_update" on storage.objects;
create policy "materials_storage_update" on storage.objects
    for update to authenticated
    using (bucket_id = 'materials' and public.is_teacher());

drop policy if exists "materials_storage_delete" on storage.objects;
create policy "materials_storage_delete" on storage.objects
    for delete to authenticated
    using (bucket_id = 'materials' and public.is_teacher());

-- ---------------------------------------------------------------------------
-- Realtime (opcional): mensajes y materiales en vivo
-- Descomenta si quieres usar Supabase Realtime en lugar de refrescar a mano.
-- ---------------------------------------------------------------------------
-- alter publication supabase_realtime add table public.messages;
-- alter publication supabase_realtime add table public.replies;
-- alter publication supabase_realtime add table public.materials;
-- alter publication supabase_realtime add table public.notifications;


-- ===== DATOS INICIALES =====

-- ============================================================================
-- ESUCAR · Datos iniciales (asignaturas, unidades y materiales)
-- Ejecutar DESPUÉS de schema.sql.
-- No crea usuarios: los usuarios se registran desde la app.
-- ============================================================================

insert into public.courses (id, code, name, description, color, position) values
    ('00000000-0000-4000-8000-000000000101', 'ADM301', 'Administración Pública',
     'Gestión pública, presupuesto y control interno en el Estado de Chile.', '#15803d', 0),
    ('00000000-0000-4000-8000-000000000102', 'SEG401', 'Seguridad Ciudadana',
     'Marco jurídico, prevención del delito y uso proporcional de la fuerza.', '#1d4ed8', 1),
    ('00000000-0000-4000-8000-000000000103', 'ETI201', 'Ética Profesional',
     'Fundamentos éticos, dilemas del servicio y responsabilidad funcionaria.', '#7c3aed', 2)
on conflict (id) do nothing;

insert into public.units (id, course_id, number, title, position) values
    ('00000000-0000-4000-8000-000000000201', '00000000-0000-4000-8000-000000000101', 1, 'Fundamentos de la Gestión Pública', 0),
    ('00000000-0000-4000-8000-000000000202', '00000000-0000-4000-8000-000000000101', 2, 'Presupuesto y Recursos del Estado', 1),
    ('00000000-0000-4000-8000-000000000203', '00000000-0000-4000-8000-000000000101', 3, 'Control y Auditoría Interna', 2),
    ('00000000-0000-4000-8000-000000000204', '00000000-0000-4000-8000-000000000102', 1, 'Marco Jurídico de la Seguridad Pública', 0),
    ('00000000-0000-4000-8000-000000000205', '00000000-0000-4000-8000-000000000102', 2, 'Prevención del Delito y Orden Público', 1),
    ('00000000-0000-4000-8000-000000000206', '00000000-0000-4000-8000-000000000102', 3, 'Uso Proporcional de la Fuerza', 2),
    ('00000000-0000-4000-8000-000000000207', '00000000-0000-4000-8000-000000000103', 1, 'Fundamentos de la Ética Policial', 0),
    ('00000000-0000-4000-8000-000000000208', '00000000-0000-4000-8000-000000000103', 2, 'Dilemas Éticos en el Servicio', 1),
    ('00000000-0000-4000-8000-000000000209', '00000000-0000-4000-8000-000000000103', 3, 'Responsabilidad y Rendición de Cuentas', 2)
on conflict (id) do nothing;

insert into public.materials
    (id, unit_id, title, type, description, file_name, mime_type, size_bytes, position)
values
    ('00000000-0000-4000-8000-000000000301', '00000000-0000-4000-8000-000000000201',
     'Introducción a la Administración del Estado', 'PDF',
     'Conceptos fundamentales sobre la estructura y funcionamiento del Estado chileno.',
     'introduccion-administracion-estado.pdf', 'application/pdf', 2516582, 0),
    ('00000000-0000-4000-8000-000000000302', '00000000-0000-4000-8000-000000000201',
     'Organigrama Institucional ESUCAR', 'PRESENTACION',
     'Presentación sobre la estructura jerárquica y roles dentro de la institución.',
     'organigrama-esucar.pdf', 'application/pdf', 5347737, 1),
    ('00000000-0000-4000-8000-000000000303', '00000000-0000-4000-8000-000000000201',
     'Principios de la Función Pública', 'GUIA',
     'Guía de ejercicios sobre los principios que rigen el actuar del servidor público.',
     'principios-funcion-publica.pdf', 'application/pdf', 1258291, 2),
    ('00000000-0000-4000-8000-000000000304', '00000000-0000-4000-8000-000000000202',
     'Ley de Presupuestos del Sector Público', 'PDF',
     'Análisis de la ley de presupuestos y su aplicación en organismos de seguridad.',
     'ley-presupuestos.pdf', 'application/pdf', 3984588, 0),
    ('00000000-0000-4000-8000-000000000305', '00000000-0000-4000-8000-000000000202',
     'Ciclo Presupuestario', 'VIDEO',
     'Clase grabada explicando las etapas del ciclo presupuestario en el sector público.',
     'ciclo-presupuestario.mp4', 'video/mp4', 15728640, 1),
    ('00000000-0000-4000-8000-000000000306', '00000000-0000-4000-8000-000000000203',
     'Normas de Control Interno - Contraloría', 'PDF',
     'Normativa vigente de la Contraloría General de la República sobre control interno.',
     'normas-control-interno.pdf', 'application/pdf', 4718592, 0),
    ('00000000-0000-4000-8000-000000000307', '00000000-0000-4000-8000-000000000203',
     'Pauta de Evaluación Unidad 3', 'GUIA',
     'Criterios de evaluación y rúbrica para la prueba de la unidad 3.',
     'pauta-evaluacion-u3.pdf', 'application/pdf', 838860, 1),
    ('00000000-0000-4000-8000-000000000308', '00000000-0000-4000-8000-000000000204',
     'Constitución Política: Orden Público', 'PDF',
     'Extracto de la Constitución Política de Chile, artículos relativos al orden público y seguridad.',
     'constitucion-orden-publico.pdf', 'application/pdf', 1992294, 0),
    ('00000000-0000-4000-8000-000000000309', '00000000-0000-4000-8000-000000000204',
     'Ley Orgánica de Carabineros', 'PDF',
     'Texto íntegro de la Ley 18.961 que establece la organización y atribuciones de Carabineros.',
     'ley-organica-carabineros.pdf', 'application/pdf', 3355443, 1),
    ('00000000-0000-4000-8000-000000000310', '00000000-0000-4000-8000-000000000204',
     'Instituciones de Seguridad', 'PRESENTACION',
     'Mapa institucional de las entidades de seguridad pública en Chile y su coordinación.',
     'instituciones-seguridad.pdf', 'application/pdf', 6606028, 2),
    ('00000000-0000-4000-8000-000000000311', '00000000-0000-4000-8000-000000000205',
     'Estrategias de Prevención Situacional', 'PDF',
     'Metodologías y herramientas para la prevención del delito en el contexto comunitario.',
     'prevencion-situacional.pdf', 'application/pdf', 2831155, 0),
    ('00000000-0000-4000-8000-000000000312', '00000000-0000-4000-8000-000000000205',
     'Patrullaje Comunitario', 'VIDEO',
     'Registro audiovisual de buenas prácticas en operativos de patrullaje comunitario.',
     'patrullaje-comunitario.mp4', 'video/mp4', 20971520, 1),
    ('00000000-0000-4000-8000-000000000313', '00000000-0000-4000-8000-000000000206',
     'Protocolos de Uso de la Fuerza', 'GUIA',
     'Manual de procedimientos para el uso proporcional y racional de la fuerza policial.',
     'protocolos-uso-fuerza.pdf', 'application/pdf', 1468006, 0),
    ('00000000-0000-4000-8000-000000000314', '00000000-0000-4000-8000-000000000207',
     'Código de Ética de Carabineros', 'PDF',
     'Documento oficial del código de conducta y valores institucionales de Carabineros de Chile.',
     'codigo-etica.pdf', 'application/pdf', 1153434, 0),
    ('00000000-0000-4000-8000-000000000315', '00000000-0000-4000-8000-000000000207',
     'Ética y Derechos Humanos', 'PRESENTACION',
     'Relación entre el actuar policial y el respeto a los derechos fundamentales de las personas.',
     'etica-derechos-humanos.pdf', 'application/pdf', 4404019, 1),
    ('00000000-0000-4000-8000-000000000316', '00000000-0000-4000-8000-000000000208',
     'Casos de Estudio: Dilemas Éticos', 'GUIA',
     'Análisis de situaciones reales que presentan conflictos éticos en el ejercicio del servicio policial.',
     'casos-dilemas-eticos.pdf', 'application/pdf', 943718, 0),
    ('00000000-0000-4000-8000-000000000317', '00000000-0000-4000-8000-000000000208',
     'Integridad Institucional', 'VIDEO',
     'Documental sobre la importancia de la integridad y transparencia en las instituciones de seguridad.',
     'integridad-institucional.mp4', 'video/mp4', 26214400, 1),
    ('00000000-0000-4000-8000-000000000318', '00000000-0000-4000-8000-000000000209',
     'Marco Legal de Responsabilidad Funcionaria', 'PDF',
     'Normativa sobre la responsabilidad civil, penal y administrativa del funcionario policial.',
     'responsabilidad-funcionaria.pdf', 'application/pdf', 2411725, 0),
    ('00000000-0000-4000-8000-000000000319', '00000000-0000-4000-8000-000000000209',
     'Guía de Evaluación Final', 'GUIA',
     'Instrucciones y criterios para la evaluación sumativa de la asignatura.',
     'evaluacion-final.pdf', 'application/pdf', 734003, 1)
on conflict (id) do nothing;

-- ---------------------------------------------------------------------------
-- Material de demostración con archivo real en Storage
-- 1) Crea un PDF pequeño y súbelo al bucket con la ruta: demo/guia-bienvenida.pdf
-- 2) Luego ejecuta el UPDATE de abajo (o créalo desde la app como docente).
-- ---------------------------------------------------------------------------
-- update public.materials
-- set storage_path = 'demo/guia-bienvenida.pdf',
--     file_name = 'guia-bienvenida.pdf',
--     mime_type = 'application/pdf'
-- where id = '00000000-0000-4000-8000-000000000301';
