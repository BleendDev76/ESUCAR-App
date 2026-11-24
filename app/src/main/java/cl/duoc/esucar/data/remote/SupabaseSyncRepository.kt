package cl.duoc.esucar.data.remote

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.core.content.FileProvider
import cl.duoc.esucar.core.time.nowTimestamp
import cl.duoc.esucar.data.local.dao.CourseDao
import cl.duoc.esucar.data.local.dao.DirectMessageDao
import cl.duoc.esucar.data.local.dao.MaterialDao
import cl.duoc.esucar.data.local.dao.MessageDao
import cl.duoc.esucar.data.local.dao.NotificationDao
import cl.duoc.esucar.data.local.entity.CourseEntity
import cl.duoc.esucar.data.local.entity.DirectMessageEntity
import cl.duoc.esucar.data.local.entity.MaterialEntity
import cl.duoc.esucar.data.local.entity.MessageEntity
import cl.duoc.esucar.data.local.entity.NotificationEntity
import cl.duoc.esucar.data.local.entity.UnitEntity
import cl.duoc.esucar.data.prefs.SessionStore
import cl.duoc.esucar.data.remote.api.SupabaseRestApi
import cl.duoc.esucar.data.remote.api.SupabaseStorageApi
import cl.duoc.esucar.data.remote.dto.CreateMaterialRequest
import cl.duoc.esucar.data.remote.dto.CreateMessageRequest
import cl.duoc.esucar.data.remote.dto.CreateReplyRequest
import cl.duoc.esucar.data.remote.dto.CreateDirectMessageRequest
import cl.duoc.esucar.data.remote.dto.UpdateMaterialRequest
import cl.duoc.esucar.data.remote.dto.UpdateReadRequest
import cl.duoc.esucar.data.remote.dto.UpsertDownloadRequest
import cl.duoc.esucar.data.remote.dto.UpsertProgressRequest
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.domain.model.TipoMaterial
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

/**
 * Archivo elegido por el docente para publicar un material.
 * Se lee con [ContentResolver] porque el selector del sistema devuelve una URI.
 */
data class ArchivoMaterial(
    val uri: Uri,
    val nombre: String,
    val mimeType: String,
    val tamanoBytes: Long
) {
    val tamanoLegible: String get() = formatBytes(tamanoBytes)

    companion object {
        fun desde(context: Context, uri: Uri): ArchivoMaterial {
            val resolver = context.contentResolver
            var nombre = uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
            var tamano = 0L

            resolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex >= 0 && !cursor.isNull(nameIndex)) nombre = cursor.getString(nameIndex)
                    if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) tamano = cursor.getLong(sizeIndex)
                }
            }

            val mime = resolver.getType(uri) ?: "application/octet-stream"
            return ArchivoMaterial(uri, nombre, mime, tamano)
        }
    }
}

/**
 * Puente entre Supabase y la caché local Room.
 *
 * Room sigue siendo la fuente que observa la UI (funciona sin conexión); este
 * repositorio se encarga de traer los datos remotos y de enviar las escrituras
 * al backend. Todas las tablas están protegidas por RLS, así que el servidor
 * decide qué ve y qué puede escribir cada usuario.
 */
class SupabaseSyncRepository(
    private val context: Context,
    private val rest: SupabaseRestApi,
    private val storage: SupabaseStorageApi,
    private val sessionStore: SessionStore,
    private val courseDao: CourseDao,
    private val materialDao: MaterialDao,
    private val messageDao: MessageDao,
    private val notificationDao: NotificationDao,
    private val directMessageDao: DirectMessageDao
) {

    /** Descarga cursos, unidades, materiales, mensajes, progreso y notificaciones. */
    suspend fun sincronizar(): Result<Unit> = runCatching {
        sincronizarCursos()
        sincronizarMensajes()
        sincronizarProgreso()
        sincronizarNotificaciones()
        sincronizarMensajesDirectos()
    }.onFailure { error ->
        // Sin este log, un fallo de sincronización queda invisible en la demo.
        Log.w("ESUCAR", "No se pudo sincronizar con Supabase: ${error.message}")
    }

    // -----------------------------------------------------------------------
    // Lectura remota -> Room
    // -----------------------------------------------------------------------

    suspend fun sincronizarCursos() {
        val remotos = rest.courses()
        val previos = materialDao.getAll().associateBy { it.id }
        val nombresDocentes = remotos.mapNotNull { it.teacherId }.distinct().associateWith { teacherId ->
            runCatching { rest.teacherDirectory("eq.$teacherId").firstOrNull()?.fullName }
                .getOrNull()
                .orEmpty()
        }

        val cursos = mutableListOf<CourseEntity>()
        val unidades = mutableListOf<UnitEntity>()
        val materiales = mutableListOf<MaterialEntity>()

        remotos.forEach { curso ->
            cursos += CourseEntity(
                id = curso.id,
                code = curso.code,
                name = curso.name,
                teacherName = nombresDocentes[curso.teacherId].orEmpty().ifBlank { TEACHER_FALLBACK },
                color = curso.color,
                position = curso.position,
                teacherId = curso.teacherId
            )
            curso.units.forEach { unidad ->
                unidades += UnitEntity(
                    id = unidad.id,
                    courseId = unidad.courseId,
                    number = unidad.number,
                    title = unidad.title,
                    position = unidad.position
                )
                unidad.materials.forEach { material ->
                    val previo = previos[material.id]
                    materiales += MaterialEntity(
                        id = material.id,
                        unitId = material.unitId,
                        title = material.title,
                        type = material.type.uppercase(Locale.ROOT),
                        size = material.sizeBytes?.let(::formatBytes),
                        // El progreso y la descarga son estado local del dispositivo.
                        studied = previo?.studied ?: false,
                        downloaded = previo?.downloaded ?: false,
                        description = material.description,
                        uploadedAt = material.createdAt?.let(::toLocalDate).orEmpty(),
                        position = material.position,
                        storagePath = material.storagePath,
                        fileName = material.fileName,
                        mimeType = material.mimeType,
                        localPath = previo?.localPath,
                        pendingProgressUserId = previo?.pendingProgressUserId
                    )
                }
            }
        }

        // El upsert evita el ON DELETE CASCADE que provocaría un REPLACE.
        courseDao.upsertCourses(cursos)
        courseDao.upsertUnits(unidades)
        materialDao.upsertAll(materiales)

        if (unidades.isEmpty()) courseDao.deleteAllUnits()
        else courseDao.deleteUnitsNotIn(unidades.map { it.id })

        // Respuesta exitosa vacía también es autoritativa. Una excepción ocurre
        // antes de este punto y, por tanto, nunca elimina la caché offline.
        if (materiales.isEmpty()) materialDao.deleteAll() else materialDao.deleteNotIn(materiales.map { it.id })
        if (cursos.isEmpty()) courseDao.deleteAllCourses() else courseDao.deleteCoursesNotIn(cursos.map { it.id })
    }

    suspend fun sincronizarMensajes() {
        val remotos = rest.messages()
        val cursos = courseDao.getCourses()

        val entidades = remotos.map { dto ->
            val curso = cursos.firstOrNull { it.course.id == dto.courseId }
            val unidad = curso?.units?.firstOrNull { it.unit.id == dto.unitId }
            val material = unidad?.materials?.firstOrNull { it.id == dto.materialId }
            val respuesta = dto.replies.maxByOrNull { it.createdAt }

            MessageEntity(
                id = dto.id,
                studentId = dto.studentId,
                studentName = dto.studentName.ifBlank { "Estudiante" },
                courseId = dto.courseId,
                courseName = curso?.course?.name.orEmpty(),
                unitId = dto.unitId.orEmpty(),
                unitTitle = unidad?.unit?.title.orEmpty(),
                materialId = dto.materialId,
                materialTitle = material?.title,
                subject = dto.subject,
                content = dto.content,
                timestamp = toLocalDateTime(dto.createdAt),
                reply = respuesta?.content,
                replyTimestamp = respuesta?.createdAt?.let(::toLocalDateTime)
            )
        }

        // El backend ya filtró por usuario; se reemplaza la caché completa.
        messageDao.clearAll()
        messageDao.upsertAll(entidades)
    }

    suspend fun sincronizarMensajesDirectos() {
        val remotos = rest.directMessages()
        directMessageDao.clearAll()
        directMessageDao.upsertAll(remotos.map { dto ->
            DirectMessageEntity(
                id = dto.id,
                studentId = dto.studentId,
                teacherId = dto.teacherId,
                senderId = dto.senderId,
                studentName = dto.studentName,
                content = dto.content,
                timestamp = toLocalDateTime(dto.createdAt)
            )
        })
    }

    suspend fun enviarMensajeDirecto(studentId: String, teacherId: String, content: String) {
        val userId = requireUserId()
        val role = sessionStore.rol.first() ?: error("No se pudo validar el rol de la cuenta.")
        require(
            (role == RolUsuario.ESTUDIANTE && studentId == userId) ||
                    (role == RolUsuario.DOCENTE && teacherId == userId)
        ) { "La conversación no pertenece a la sesión actual." }
        val name = if (role == RolUsuario.ESTUDIANTE) {
            sessionStore.userName.first().orEmpty().ifBlank { "Estudiante" }
        } else {
            rest.profile("eq.$studentId").firstOrNull()?.fullName?.ifBlank { "Estudiante" }
                ?: "Estudiante"
        }
        rest.createDirectMessage(
            CreateDirectMessageRequest(
                studentId = studentId,
                teacherId = teacherId,
                senderId = userId,
                studentName = name,
                content = content.trim()
            )
        )
        sincronizarMensajesDirectos()
    }

    suspend fun sincronizarProgreso() {
        if (sessionStore.rol.first() != RolUsuario.ESTUDIANTE) return

        val userId = requireUserId()
        // Reintenta el outbox antes de aplicar el estado remoto. Solo se borra
        // cada pendiente después de que Supabase confirme el upsert.
        materialDao.getPendingProgressIds(userId).forEach { materialId ->
            rest.upsertProgress(UpsertProgressRequest(studentId = userId, materialId = materialId))
            materialDao.clearPendingProgress(materialId, userId)
        }
        val remoto = rest.progress()
        materialDao.resetStudiedExceptPending(userId)
        remoto.forEach { materialDao.markStudied(it.materialId) }
    }

    suspend fun sincronizarNotificaciones() {
        val rol = sessionStore.rol.first() ?: return
        val remotos = rest.notifications()

        notificationDao.clearAll()
        notificationDao.upsertAll(
            remotos.map { dto ->
                NotificationEntity(
                    id = dto.id,
                    role = rol.name,
                    title = dto.title,
                    description = dto.body,
                    timestamp = toLocalDateTime(dto.createdAt),
                    read = dto.readAt != null,
                    type = dto.type.uppercase(Locale.ROOT)
                )
            }
        )
    }

    // -----------------------------------------------------------------------
    // Escrituras -> Supabase
    // -----------------------------------------------------------------------

    suspend fun enviarConsulta(
        nombreEstudiante: String,
        asignaturaId: String,
        unidadId: String,
        materialId: String?,
        asunto: String,
        contenido: String
    ) {
        val userId = requireUserId()
        rest.createMessage(
            CreateMessageRequest(
                studentId = userId,
                studentName = nombreEstudiante.trim().ifBlank { "Estudiante" },
                courseId = asignaturaId,
                unitId = unidadId,
                materialId = materialId,
                subject = asunto.trim(),
                content = contenido.trim()
            )
        )
        sincronizarMensajes()
    }

    suspend fun responderConsulta(mensajeId: String, respuesta: String) {
        val userId = requireUserId()
        rest.createReply(
            CreateReplyRequest(
                messageId = mensajeId,
                teacherId = userId,
                content = respuesta.trim()
            )
        )
        sincronizarMensajes()
    }

    suspend fun marcarEstudiado(materialId: String) {
        val userId = requireUserId()
        materialDao.markStudiedPending(materialId, userId)
        rest.upsertProgress(UpsertProgressRequest(studentId = userId, materialId = materialId))
        materialDao.clearPendingProgress(materialId, userId)
    }

    /** Publica el material y, si viene un archivo, lo sube al bucket privado. */
    suspend fun agregarMaterial(
        unidadId: String,
        titulo: String,
        tipo: TipoMaterial,
        descripcion: String,
        archivo: ArchivoMaterial?
    ) {
        val userId = requireUserId()
        val storagePath = archivo?.let { subirArchivo(unidadId, it) }
        val posicion = (materialDao.maxPosition(unidadId) ?: 0) + 1

        rest.createMaterial(
            CreateMaterialRequest(
                unitId = unidadId,
                title = titulo.trim(),
                type = tipo.name,
                description = descripcion.trim(),
                storagePath = storagePath,
                fileName = archivo?.nombre,
                mimeType = archivo?.mimeType,
                sizeBytes = archivo?.tamanoBytes,
                uploadedBy = userId,
                position = posicion
            )
        )
        sincronizarCursos()
        sincronizarNotificaciones()
    }

    suspend fun editarMaterial(
        materialId: String,
        titulo: String,
        tipo: TipoMaterial,
        descripcion: String,
        archivo: ArchivoMaterial?
    ) {
        val actual = materialDao.getById(materialId)
        val storagePath = archivo?.let { subirArchivo(actual?.unitId.orEmpty(), it) }
            ?: actual?.storagePath

        rest.updateMaterial(
            idFilter = "eq.$materialId",
            body = UpdateMaterialRequest(
                title = titulo.trim(),
                type = tipo.name,
                description = descripcion.trim(),
                storagePath = storagePath,
                fileName = archivo?.nombre ?: actual?.fileName,
                mimeType = archivo?.mimeType ?: actual?.mimeType,
                sizeBytes = archivo?.tamanoBytes
            )
        )
        sincronizarCursos()
    }

    suspend fun eliminarMaterial(materialId: String) {
        val actual = materialDao.getById(materialId)
        rest.deleteMaterial("eq.$materialId")
        actual?.storagePath?.let { runCatching { storage.delete(it) } }
        materialDao.deleteById(materialId)
    }

    suspend fun marcarNotificacionLeida(notificacionId: String) {
        notificationDao.markRead(notificacionId)
        runCatching {
            rest.markNotificationRead("eq.$notificacionId", UpdateReadRequest(ahoraIso()))
        }
    }

    suspend fun marcarTodasLeidas(rol: RolUsuario) {
        notificationDao.markAllRead(rol.name)
        runCatching { rest.markAllNotificationsRead(body = UpdateReadRequest(ahoraIso())) }
    }

    // -----------------------------------------------------------------------
    // Archivos (Storage) y modo sin conexión
    // -----------------------------------------------------------------------

    /** Descarga el archivo del material a `filesDir/materials` y lo marca offline. */
    suspend fun descargarMaterial(materialId: String): Result<File> = runCatching {
        val material = materialDao.getById(materialId)
            ?: error("El material ya no está disponible.")
        val rutaRemota = material.storagePath
            ?: error("Este material no tiene un archivo adjunto.")
        val userId = requireUserId()

        val cuerpo = storage.download(rutaRemota)
        val carpeta = File(context.filesDir, "materials").apply { mkdirs() }
        val destino = uniqueFile(carpeta, material.fileName ?: rutaRemota.substringAfterLast('/'))

        cuerpo.byteStream().use { entrada ->
            destino.outputStream().use { salida -> entrada.copyTo(salida) }
        }

        materialDao.setLocalFile(materialId, destino.absolutePath, true)
        runCatching {
            rest.upsertDownload(UpsertDownloadRequest(studentId = userId, materialId = materialId))
        }
        destino
    }

    /** Elimina la copia local y el registro de descarga remoto. */
    suspend fun quitarDescarga(materialId: String) {
        val material = materialDao.getById(materialId)
        material?.localPath?.let { ruta -> runCatching { File(ruta).delete() } }
        materialDao.setLocalFile(materialId, null, false)
        runCatching {
            val userId = requireUserId()
            rest.deleteDownload("eq.$userId", "eq.$materialId")
        }
    }

    /** Abre el archivo descargado con el visor del sistema (FileProvider). */
    suspend fun abrirMaterial(materialId: String): Result<Unit> = runCatching {
        val material = materialDao.getById(materialId)
            ?: error("El material ya no está disponible.")
        val archivo = material.localPath?.let(::File)
        require(archivo != null && archivo.exists()) {
            "Descarga el material primero para abrirlo sin conexión."
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, material.mimeType ?: "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private suspend fun subirArchivo(unidadId: String, archivo: ArchivoMaterial): String {
        val original = archivo.nombre.substringAfterLast('/').ifBlank { "material" }
        val seguro = original.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val ruta = "${unidadId.ifBlank { "general" }}/${UUID.randomUUID()}-$seguro"

        val bytes = context.contentResolver.openInputStream(archivo.uri)?.use { it.readBytes() }
            ?: error("No se pudo leer el archivo seleccionado.")
        val cuerpo = bytes.toRequestBody(archivo.mimeType.toMediaTypeOrNull())
        storage.upload(ruta, cuerpo)
        return ruta
    }

    private suspend fun requireUserId(): String =
        sessionStore.userId.first() ?: error("No hay una sesión activa.")

    /** Marca de tiempo con offset para columnas `timestamptz`. */
    private fun ahoraIso(): String = OffsetDateTime.now().withNano(0).toString()

    private fun uniqueFile(carpeta: File, nombre: String): File {
        val limpio = nombre.substringAfterLast('/').ifBlank { "material" }
        var destino = File(carpeta, limpio)
        var indice = 1
        while (destino.exists()) {
            val base = limpio.substringBeforeLast('.', limpio)
            val extension = limpio.substringAfterLast('.', "")
            val sufijo = if (extension.isBlank()) "" else ".$extension"
            destino = File(carpeta, "$base-$indice$sufijo")
            indice++
        }
        return destino
    }

    private companion object {
        const val TEACHER_FALLBACK = "Docente ESUCAR"
    }
}

/** `2026-11-17T22:39:08.123+00:00` -> `2026-11-17T19:39:08` en la zona del equipo. */
internal fun toLocalDateTime(iso: String): String = runCatching {
    OffsetDateTime.parse(iso)
        .atZoneSameInstant(ZoneId.systemDefault())
        .toLocalDateTime()
        .withNano(0)
        .toString()
}.getOrElse { iso }

internal fun toLocalDate(iso: String): String = runCatching {
    OffsetDateTime.parse(iso)
        .atZoneSameInstant(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}.getOrElse { iso.take(10) }

/** Convierte bytes a un texto compacto, por ejemplo `2,4 MB`. */
internal fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale("es", "CL"), "%.0f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format(Locale("es", "CL"), "%.1f MB", mb)
    return String.format(Locale("es", "CL"), "%.1f GB", mb / 1024.0)
}
