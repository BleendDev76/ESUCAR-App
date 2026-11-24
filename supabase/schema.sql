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

-- Los clientes autenticados consultan solo id, nombre y rol del perfil.
revoke select on public.profiles from authenticated;
grant select (id, full_name, role) on public.profiles to authenticated;

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

drop policy if exists "profiles_select_teacher_directory" on public.profiles;
create policy "profiles_select_teacher_directory" on public.profiles
    for select to authenticated
    using (role = 'DOCENTE');

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

-- Chat directo alumno-docente
create table if not exists public.direct_messages (
    id uuid primary key default gen_random_uuid(),
    student_id uuid not null references public.profiles(id) on delete cascade,
    teacher_id uuid not null references public.profiles(id) on delete cascade,
    sender_id uuid not null references public.profiles(id) on delete cascade,
    student_name text not null default '',
    content text not null check (char_length(btrim(content)) between 1 and 4000),
    created_at timestamptz not null default now(),
    constraint direct_messages_distinct_participants check (student_id <> teacher_id),
    constraint direct_messages_sender_is_participant check (sender_id in (student_id, teacher_id))
);

create index if not exists direct_messages_student_thread_idx
    on public.direct_messages(student_id, teacher_id, created_at);
create index if not exists direct_messages_teacher_thread_idx
    on public.direct_messages(teacher_id, student_id, created_at);

alter table public.direct_messages enable row level security;
revoke all on public.direct_messages from anon;
grant select, insert on public.direct_messages to authenticated;

drop policy if exists "direct_messages_select_participant" on public.direct_messages;
create policy "direct_messages_select_participant" on public.direct_messages
    for select to authenticated
    using (student_id = auth.uid() or teacher_id = auth.uid());

drop policy if exists "direct_messages_insert_student" on public.direct_messages;
create policy "direct_messages_insert_student" on public.direct_messages
    for insert to authenticated
    with check (
        sender_id = auth.uid()
        and student_id = auth.uid()
        and public.my_role() = 'ESTUDIANTE'
        and exists (
            select 1 from public.courses c
            where c.teacher_id = direct_messages.teacher_id
        )
    );

drop policy if exists "direct_messages_insert_teacher" on public.direct_messages;
create policy "direct_messages_insert_teacher" on public.direct_messages
    for insert to authenticated
    with check (
        sender_id = auth.uid()
        and teacher_id = auth.uid()
        and public.my_role() = 'DOCENTE'
        and exists (
            select 1 from public.profiles p
            where p.id = direct_messages.student_id
              and p.role = 'ESTUDIANTE'
        )
        and exists (
            select 1 from public.direct_messages first_message
            where first_message.student_id = direct_messages.student_id
              and first_message.teacher_id = direct_messages.teacher_id
              and first_message.sender_id = first_message.student_id
        )
    );

create or replace function public.notify_direct_message()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    insert into public.notifications (user_id, type, title, body, related_id)
    values (
        case when new.sender_id = new.student_id then new.teacher_id else new.student_id end,
        case when new.sender_id = new.student_id then 'CONSULTA' else 'RESPUESTA' end,
        case when new.sender_id = new.student_id then 'Nuevo mensaje directo' else 'Nuevo mensaje del docente' end,
        new.content,
        new.id
    );
    return new;
end;
$$;

drop trigger if exists direct_messages_notify on public.direct_messages;
create trigger direct_messages_notify
    after insert on public.direct_messages
    for each row execute function public.notify_direct_message();

revoke all on function public.notify_direct_message() from public, anon, authenticated;

-- Directorio limitado a id y nombre del docente para el selector de chat.
create or replace view public.teacher_directory
with (security_invoker = true) as
select id, full_name
from public.profiles
where role::text = 'DOCENTE';

revoke all on public.teacher_directory from public, anon;
grant select on public.teacher_directory to authenticated;
