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
