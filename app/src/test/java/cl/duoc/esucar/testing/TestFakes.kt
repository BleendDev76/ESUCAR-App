package cl.duoc.esucar.testing

import cl.duoc.esucar.core.notifications.NotificadorSistema
import cl.duoc.esucar.data.local.dao.CourseDao
import cl.duoc.esucar.data.local.dao.MaterialDao
import cl.duoc.esucar.data.local.dao.MessageDao
import cl.duoc.esucar.data.local.dao.NotificationDao
import cl.duoc.esucar.data.local.entity.CourseEntity
import cl.duoc.esucar.data.local.entity.MaterialEntity
import cl.duoc.esucar.data.local.entity.MessageEntity
import cl.duoc.esucar.data.local.entity.NotificationEntity
import cl.duoc.esucar.data.local.entity.UnitEntity
import cl.duoc.esucar.data.local.relation.CourseWithUnits
import cl.duoc.esucar.data.local.relation.UnitWithMaterials
import cl.duoc.esucar.domain.model.TipoMaterial
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class FakeNotificador : NotificadorSistema {

    data class Llamada(val titulo: String, val descripcion: String)

    val llamadas = mutableListOf<Llamada>()

    override fun notificar(titulo: String, descripcion: String) {
        llamadas += Llamada(titulo, descripcion)
    }
}

internal class FakeCourseDao(private val cursos: List<CourseWithUnits>) : CourseDao {

    override fun observeCourses(): Flow<List<CourseWithUnits>> = flowOf(cursos)

    override fun observeCoursesForChat(): Flow<List<CourseWithUnits>> = flowOf(cursos)

    override suspend fun getCourses(): List<CourseWithUnits> = cursos

    override suspend fun getCourse(courseId: String): CourseWithUnits? =
        cursos.firstOrNull { it.course.id == courseId }

    override suspend fun getAllCourses(): List<CourseEntity> = cursos.map { it.course }

    override suspend fun upsertCourses(courses: List<CourseEntity>) = Unit

    override suspend fun upsertUnits(units: List<UnitEntity>) = Unit

    override suspend fun deleteUnitsNotIn(ids: List<String>) = Unit

    override suspend fun deleteAllUnits() = Unit

    override suspend fun deleteCoursesNotIn(ids: List<String>) = Unit

    override suspend fun deleteAllCourses() = Unit
}

internal class FakeMaterialDao(
    iniciales: List<MaterialEntity> = emptyList()
) : MaterialDao {

    private val materiales = iniciales.toMutableList()

    override fun observeByUnit(unitId: String): Flow<List<MaterialEntity>> =
        flowOf(materiales.filter { it.unitId == unitId })

    override fun observeDownloaded(): Flow<List<MaterialEntity>> =
        flowOf(materiales.filter { it.downloaded })

    override suspend fun getById(materialId: String): MaterialEntity? =
        materiales.firstOrNull { it.id == materialId }

    override suspend fun minPosition(unitId: String): Int? =
        materiales.filter { it.unitId == unitId }.minOfOrNull { it.position }

    override suspend fun maxPosition(unitId: String): Int? =
        materiales.filter { it.unitId == unitId }.maxOfOrNull { it.position }

    override suspend fun getAll(): List<MaterialEntity> = materiales.toList()

    override suspend fun upsertAll(materials: List<MaterialEntity>) {
        materials.forEach { material ->
            val indice = materiales.indexOfFirst { it.id == material.id }
            if (indice >= 0) materiales[indice] = material else materiales += material
        }
    }

    override suspend fun deleteNotIn(ids: List<String>) {
        materiales.removeAll { it.id !in ids }
    }

    override suspend fun deleteAll() { materiales.clear() }

    override suspend fun resetStudied() {
        materiales.replaceAll { it.copy(studied = false) }
    }

    override suspend fun resetStudiedExceptPending(userId: String) {
        materiales.replaceAll {
            if (it.pendingProgressUserId == userId) it else it.copy(studied = false)
        }
    }

    override suspend fun markStudiedPending(materialId: String, userId: String) {
        actualizar(materialId) { it.copy(studied = true, pendingProgressUserId = userId) }
    }

    override suspend fun getPendingProgressIds(userId: String): List<String> =
        materiales.filter { it.pendingProgressUserId == userId }.map { it.id }

    override suspend fun clearPendingProgress(materialId: String, userId: String) {
        actualizar(materialId) {
            if (it.pendingProgressUserId == userId) it.copy(pendingProgressUserId = null) else it
        }
    }

    override suspend fun setLocalFile(materialId: String, localPath: String?, downloaded: Boolean) {
        actualizar(materialId) { it.copy(localPath = localPath, downloaded = downloaded) }
    }

    override suspend fun insert(material: MaterialEntity) {
        materiales += material
    }

    override suspend fun update(material: MaterialEntity) {
        val indice = materiales.indexOfFirst { it.id == material.id }
        if (indice >= 0) materiales[indice] = material
    }

    override suspend fun deleteById(materialId: String) {
        materiales.removeAll { it.id == materialId }
    }

    override suspend fun markStudied(materialId: String) {
        actualizar(materialId) { it.copy(studied = true) }
    }

    override suspend fun setDownloaded(materialId: String, downloaded: Boolean) {
        actualizar(materialId) { it.copy(downloaded = downloaded) }
    }

    override suspend fun toggleDownload(materialId: String) {
        actualizar(materialId) { it.copy(downloaded = !it.downloaded) }
    }

    private fun actualizar(materialId: String, cambio: (MaterialEntity) -> MaterialEntity) {
        val indice = materiales.indexOfFirst { it.id == materialId }
        if (indice >= 0) materiales[indice] = cambio(materiales[indice])
    }
}

internal class FakeMessageDao(
    iniciales: List<MessageEntity> = emptyList()
) : MessageDao {

    private val mensajes = iniciales.toMutableList()

    override fun observeAll(): Flow<List<MessageEntity>> = flowOf(mensajes.toList())

    override fun observeById(messageId: String): Flow<MessageEntity?> =
        flowOf(mensajes.firstOrNull { it.id == messageId })

    override suspend fun getById(messageId: String): MessageEntity? =
        mensajes.firstOrNull { it.id == messageId }

    override suspend fun insert(message: MessageEntity) {
        mensajes += message
    }

    override suspend fun clearAll() {
        mensajes.clear()
    }

    override suspend fun upsertAll(messages: List<MessageEntity>) {
        messages.forEach { mensaje ->
            val indice = mensajes.indexOfFirst { it.id == mensaje.id }
            if (indice >= 0) mensajes[indice] = mensaje else mensajes += mensaje
        }
    }

    override suspend fun setReply(messageId: String, reply: String, replyTimestamp: String) {
        val indice = mensajes.indexOfFirst { it.id == messageId }
        if (indice >= 0) {
            mensajes[indice] = mensajes[indice].copy(reply = reply, replyTimestamp = replyTimestamp)
        }
    }
}

internal class FakeNotificationDao : NotificationDao {

    val notificaciones = mutableListOf<NotificationEntity>()

    override fun observeByRole(role: String): Flow<List<NotificationEntity>> =
        flowOf(notificaciones.filter { it.role == role })

    override fun observeUnreadCount(role: String): Flow<Int> =
        flowOf(notificaciones.count { it.role == role && !it.read })

    override suspend fun insert(notification: NotificationEntity) {
        notificaciones += notification
    }

    override suspend fun getAllByRole(role: String): List<NotificationEntity> =
        notificaciones.filter { it.role == role }

    override suspend fun upsertAll(notifications: List<NotificationEntity>) {
        notifications.forEach { notificacion ->
            val indice = notificaciones.indexOfFirst { it.id == notificacion.id }
            if (indice >= 0) notificaciones[indice] = notificacion else notificaciones += notificacion
        }
    }

    override suspend fun clearAll() {
        notificaciones.clear()
    }

    override suspend fun markRead(notificationId: String) {
        val indice = notificaciones.indexOfFirst { it.id == notificationId }
        if (indice >= 0) notificaciones[indice] = notificaciones[indice].copy(read = true)
    }

    override suspend fun markAllRead(role: String) {
        notificaciones.replaceAll { notificacion ->
            if (notificacion.role == role) notificacion.copy(read = true) else notificacion
        }
    }
}

internal fun cursoDePrueba(
    materiales: List<MaterialEntity> = listOf(materialDePrueba())
): CourseWithUnits =
    CourseWithUnits(
        course = CourseEntity(
            id = "course-1",
            code = "DSY1105",
            name = "Aplicaciones Móviles",
            teacherName = "Docente de prueba",
            color = "#15803d",
            position = 1
        ),
        units = listOf(
            UnitWithMaterials(
                unit = UnitEntity(
                    id = "unit-1",
                    courseId = "course-1",
                    number = 1,
                    title = "Introducción",
                    position = 1
                ),
                materials = materiales
            )
        )
    )

internal fun materialDePrueba(
    id: String = "material-1",
    estudiado: Boolean = false,
    descargado: Boolean = false
): MaterialEntity =
    MaterialEntity(
        id = id,
        unitId = "unit-1",
        title = "Guía inicial",
        type = TipoMaterial.GUIA.name,
        size = "1 MB",
        studied = estudiado,
        downloaded = descargado,
        description = "Descripción",
        uploadedAt = "2026-10-25",
        position = id.filter { it.isDigit() }.toIntOrNull() ?: 1
    )

internal fun consultaDePrueba(id: String = "message-1"): MessageEntity =
    MessageEntity(
        id = id,
        studentName = "Ana Pérez",
        courseId = "course-1",
        courseName = "Aplicaciones Móviles",
        unitId = "unit-1",
        unitTitle = "Introducción",
        materialId = null,
        materialTitle = null,
        subject = "Consulta inicial",
        content = "¿Cuándo es la prueba?",
        timestamp = "2026-10-25T09:00:00",
        reply = null,
        replyTimestamp = null
    )
