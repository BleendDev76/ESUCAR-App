# ESUCAR — Repositorio móvil (DSY1105)

App Android en Kotlin + Jetpack Compose para la Escuela de Suboficiales de Carabineros.
Los estudiantes consultan material de estudio y le preguntan al docente; el docente
responde y publica materiales con su archivo.

**Backend: Supabase** (Postgres + Auth + Storage + RLS) consumido con Retrofit/OkHttp.
**Room** queda como caché local: la app abre y muestra lo último descargado aunque no
haya conexión.

## Requisitos

- Android Studio con soporte para AGP 9.2.1 (Gradle 9.4.1 se descarga solo).
- **JDK 21** (el proyecto fija `toolchainVersion=21`).
- SDK de Android con la plataforma 37.1 instalada.
- `minSdk 24`, `targetSdk 36`, `compileSdk 37.1`.
- Un proyecto Supabase con `supabase/setup_completo.sql` ejecutado
  (ver `docs/SUPABASE_SETUP.md`).

## Configuración

Copie `.env.example` como `.env` y complete la URL y la clave pública del proyecto:

```properties
SUPABASE_URL=https://<proyecto>.supabase.co
SUPABASE_ANON_KEY=<anon / publishable key>
```

Android Studio sigue usando `local.properties` solo para la ruta local del SDK:

```properties
sdk.dir=C\:\\Users\\tu-usuario\\AppData\\Local\\Android\\Sdk
```

Gradle las expone como `BuildConfig.SUPABASE_URL` y `BuildConfig.SUPABASE_ANON_KEY`.

> **Nunca** uses la `service_role` key en la app. Esa clave salta RLS y solo debe
> existir en el panel de Supabase. La `anon`/`publishable` es pública por diseño:
> lo que protege los datos son las políticas RLS.

Si no hay credenciales, la app arranca en **modo demostración** con los datos semilla
de Room (login local). Es el comportamiento del prototipo de la Evaluación 2.

## Cómo correr

```bash
./gradlew test assembleDebug     # Linux / macOS
gradlew.bat test assembleDebug   # Windows
./gradlew installDebug
```

También se puede usar **Run ▶** desde Android Studio.

Para la entrega, el APK firmado se genera con `gradlew.bat assembleRelease`; el paso a
paso y el manejo del KeyStore están en `docs/APK_FIRMADO.md`.

## Usuarios demo

Las cuentas demo se crean con las credenciales privadas del equipo. Las credenciales
actuales no se publican en este repositorio; consulta `docs/private/CREDENCIALES.txt`
solo en tu copia local autorizada.

Cualquier persona puede registrarse como **estudiante**. Para ser **docente** hay que
escribir el código de invitación: lo valida un trigger de Postgres, no el cliente.

## Qué hace cada rol

- **Estudiante:** asignaturas → unidades → materiales, marcar como estudiado (progreso
  propio), descargar el archivo y abrirlo sin conexión, consultar al docente y ver la
  respuesta y las notificaciones.
- **Docente:** bandeja de consultas con filtros, responder, publicar materiales con
  archivo adjunto y editar/eliminar los existentes.

La sesión persiste entre reinicios (tokens en DataStore). **Cerrar sesión limpia los
tokens** y vuelve al login.

## Arquitectura

```
Compose UI → ViewModel → StudyRepository
                          ├── SupabaseStudyRepository  (escrituras + sincronización)
                          │     ├── Retrofit/OkHttp → Supabase Auth/PostgREST/Storage
                          │     └── SupabaseSyncRepository → Room
                          └── RoomStudyRepository (lecturas reactivas, modo offline)
```

- **Lecturas:** siempre desde Room (`Flow`), así la UI es reactiva y funciona offline.
- **Escrituras:** van a Supabase; tras la respuesta se refresca Room.
- **Archivos:** bucket privado `materials`; se descargan a `filesDir/materials/` y se
  abren con `FileProvider` + visor del sistema.
- **Seguridad:** toda la autorización vive en RLS. El rol se lee de `profiles` después
  del login, nunca de `user_metadata`.

```
app/src/main/java/cl/duoc/esucar/
├── MainActivity.kt          punto de entrada (una sola Activity, todo Compose)
├── ESUCARApplication.kt     Application + AppContainer (inyección manual, sin Hilt)
├── core/                    conectividad, hash de contraseñas, formato de fechas
├── data/
│   ├── local/               Room: entidades, DAOs, relaciones, migraciones y semilla
│   ├── prefs/               DataStore: rol, nombre, userId y tokens JWT
│   ├── remote/              Supabase: DTOs, APIs, auth, sync y Storage
│   └── repository/          reglas de negocio y mapeo entidad ↔ dominio
├── domain/model/            modelos de la UI y cálculo de progreso
└── ui/
    ├── navigation/          rutas y grafos por rol
    ├── screens/             una carpeta por pantalla con su ViewModel
    ├── components/          piezas reutilizables (header, pills, diálogos)
    └── theme/               colores, tipografía y formas
```

La dependencia va en un solo sentido: **UI → ViewModel → Repositorio → Datos**.

## Decisiones que conviene saber antes de tocar código

- Las columnas de Room están **en inglés** y los modelos de dominio en español. El mapper
  del repositorio es la frontera; no renombrar columnas porque rompe bases ya instaladas.
- Room está en **versión 3**: las migraciones `1→2` (tabla `users`) y `2→3` (metadatos
  de archivo y `studentId` en mensajes) están en `AppDatabase`.
- Los `@Upsert` usan `INSERT ... ON CONFLICT DO UPDATE`. **No** se usa
  `OnConflictStrategy.REPLACE` en cursos/unidades porque REPLACE borra la fila y
  dispararía el `ON DELETE CASCADE`.
- El progreso **no se guarda como porcentaje**: se deriva de los materiales marcados,
  y en remoto es una fila por material en `progress`.
- Marcar un material como estudiado es **irreversible** desde la UI.
- Las claves de DataStore `role`, `userName`, `accessToken`, `refreshToken` y
  `supabaseUserId` no se renombran: hay sesiones guardadas.
- El hash local de contraseñas (modo demo) es PBKDF2-HMAC-SHA1 con 150 000 iteraciones.

## Tests

```bash
./gradlew test
```

- `PasswordHasherTest` (3): hash, verificación y salt distinto por usuario.
- `UserRepositoryTest` (4): registro, correo duplicado y login.
- `RoomStudyRepositoryTest` (4): progreso 7/19 = 37 %, notificaciones al enviar y responder,
  alternancia de descarga.
- `NotificadorSistemaTest` (3): notificación del sistema en los tres eventos.

Los fakes compartidos viven en `app/src/test/java/cl/duoc/esucar/testing/TestFakes.kt`.

## Documentación

- `docs/SUPABASE_SETUP.md` — crear el proyecto, ejecutar el SQL y desactivar la
  confirmación de correo.
- `docs/E3_PLAN.md` — rúbrica, arquitectura, modelo de datos, demo y checklist.
- `docs/COMPARACION_PROFESOR.md` — equivalencias con el proyecto `AppProductos` del
  profesor y respuestas para la defensa.
- `docs/APK_FIRMADO.md` — KeyStore, firma release y verificación con `apksigner`.
- `supabase/schema.sql` — esquema, RLS, triggers y bucket.
- `supabase/seed.sql` — 3 asignaturas, 9 unidades y 19 materiales de ejemplo.
