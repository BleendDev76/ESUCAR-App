-- Allow the authenticated client to look up only public teacher profile fields.
revoke select on public.profiles from authenticated;
grant select (id, full_name, role) on public.profiles to authenticated;

drop policy if exists "profiles_select_teacher_directory" on public.profiles;
create policy "profiles_select_teacher_directory" on public.profiles
    for select to authenticated
    using (role = 'DOCENTE');

create or replace view public.teacher_directory
with (security_invoker = true) as
select id, full_name
from public.profiles
where role::text = 'DOCENTE';

revoke all on public.teacher_directory from public, anon;
grant select on public.teacher_directory to authenticated;
