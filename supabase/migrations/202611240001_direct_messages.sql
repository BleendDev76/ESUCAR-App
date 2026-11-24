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
