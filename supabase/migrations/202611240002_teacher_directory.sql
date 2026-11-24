-- Lista de docentes disponibles sin exponer otros datos del perfil.
create or replace view public.teacher_directory
with (security_invoker = false) as
select id, full_name
from public.profiles
where role::text = 'DOCENTE';

revoke all on public.teacher_directory from public, anon;
grant select on public.teacher_directory to authenticated;
