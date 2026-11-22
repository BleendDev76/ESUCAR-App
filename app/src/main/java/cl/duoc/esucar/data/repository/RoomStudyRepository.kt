package cl.duoc.esucar.data.repository

import cl.duoc.esucar.core.notifications.NotificadorSistema
import cl.duoc.esucar.core.time.nowTimestamp
import cl.duoc.esucar.core.time.todayDate
import cl.duoc.esucar.data.local.dao.CourseDao
import cl.duoc.esucar.data.local.dao.MaterialDao
import cl.duoc.esucar.data.local.dao.MessageDao
import cl.duoc.esucar.data.local.dao.NotificationDao
import cl.duoc.esucar.data.local.entity.MaterialEntity
import cl.duoc.esucar.data.local.entity.MessageEntity
import cl.duoc.esucar.data.local.entity.NotificationEntity
import cl.duoc.esucar.data.local.relation.CourseWithUnits
import cl.duoc.esucar.data.local.relation.UnitWithMaterials
import cl.duoc.esucar.data.remote.ArchivoMaterial
import cl.duoc.esucar.domain.model.Asignatura
import cl.duoc.esucar.domain.model.Material
import cl.duoc.esucar.domain.model.Mensaje
import cl.duoc.esucar.domain.model.Notificacion
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.domain.model.TipoMaterial
import cl.duoc.esucar.domain.model.TipoNotificacion
import cl.duoc.esucar.domain.model.Unidad
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomStudyRepository(
    private val courseDao: CourseDao,
    private val materialDao: MaterialDao,
    private val messageDao: MessageDao,
    private val notificationDao: NotificationDao,
    private val timestampProvider: () -> String = ::nowTimestamp,
    private val dateProvider: () -> String = ::todayDate,
    private val idProvider: () -> Long = { System.currentTimeMillis() },
    private val notificador: NotificadorSistema = NotificadorSistema.Ninguno
) : StudyRepository {

    override fun observarAsignaturas(): Flow<List<Asignatura>> =
        courseDao.observeCourses().map { asignaturas ->
            asignaturas.map { it.toDomain() }
        }

    override fun observarMensajes(): Flow<List<Mensaje>> =
        messageDao.observeAll().map { mensajes ->
            mensajes.map { it.toDomain() }
        }

    override fun observarMensaje(mensajeId: String): Flow<Mensaje?> =
        messageDao.observeById(mensajeId).map { it?.toDomain() }

    override fun observarNotificaciones(rol: RolUsuario): Flow<List<Notificacion>> =
        notificationDao.observeByRole(rol.name).map { notificaciones ->
            notificaciones.map { it.toDomain() }
        }

    override fun observarNoLeidas(rol: RolUsuario): Flow<Int> =
        notificationDao.observeUnreadCount(rol.name)

    override suspend fun obtenerMaterial(materialId: String): Material? =
        materialDao.getById(materialId)?.toDomain()

    override suspend fun marcarEstudiado(materialId: String) {
        materialDao.markStudied(materialId)
    }

    override suspend fun alternarDescarga(materialId: String) {
        materialDao.toggleDownload(materialId)
    }

    override suspend fun enviarConsulta(
        nombreEstudiante: String,
        asignaturaId: String,
        unidadId: String,
        materialId: String?,
        asunto: String,
        contenido: String
    ) {
        val asuntoLimpio = asunto.trim()
        val contenidoLimpio = contenido.trim()
        require(asuntoLimpio.isNotEmpty() && contenidoLimpio.isNotEmpty()) {
            "El asunto y el mensaje son obligatorios."
        }

        val asignatura = courseDao.getCourse(asignaturaId) ?: error("Asignatura no encontrada.")
        val unidad = buscarUnidad(asignatura, unidadId)
        val tituloMaterial = materialId?.let { idMaterial ->
            unidad.materials.firstOrNull { it.id == idMaterial }?.title
        }
        val fecha = timestampProvider()

        messageDao.insert(
            MessageEntity(
                id = "message-${idProvider()}",
                studentName = nombreEstudiante.trim().ifBlank { "Estudiante" },
                courseId = asignatura.course.id,
                courseName = asignatura.course.name,
                unitId = unidad.unit.id,
                unitTitle = unidad.unit.title,
                materialId = materialId,
                materialTitle = tituloMaterial,
                subject = asuntoLimpio,
                content = contenidoLimpio,
                timestamp = fecha,
                reply = null,
                replyTimestamp = null
            )
        )

        insertarNotificacion(
            rol = RolUsuario.DOCENTE,
            titulo = "Nueva consulta de estudiante",
            descripcion = "$asuntoLimpio · ${asignatura.course.name}",
            tipo = TipoNotificacion.CONSULTA,
            fecha = fecha
        )
    }

    override suspend fun responderConsulta(mensajeId: String, respuesta: String) {
        val respuestaLimpia = respuesta.trim()
        require(respuestaLimpia.isNotEmpty()) { "La respuesta no puede estar vacía." }

        val consulta = messageDao.getById(mensajeId)
            ?: error("Consulta no encontrada.")
        val fecha = timestampProvider()

        messageDao.setReply(mensajeId, respuestaLimpia, fecha)

        insertarNotificacion(
            rol = RolUsuario.ESTUDIANTE,
            titulo = "Tu consulta fue respondida",
            descripcion = "${consulta.subject} · ${consulta.courseName}",
            tipo = TipoNotificacion.RESPUESTA,
            fecha = fecha
        )
    }

    override suspend fun agregarMaterial(
        asignaturaId: String,
        unidadId: String,
        titulo: String,
        tipo: TipoMaterial,
        descripcion: String,
        tamano: String?,
        archivo: ArchivoMaterial?
    ) {
        val tituloLimpio = titulo.trim()
        val descripcionLimpia = descripcion.trim()
        require(tituloLimpio.isNotEmpty() && descripcionLimpia.isNotEmpty()) {
            "El título y la descripción son obligatorios."
        }

        val asignatura = courseDao.getCourse(asignaturaId) ?: error("Asignatura no encontrada.")
        val unidad = buscarUnidad(asignatura, unidadId)

        val posicion = (materialDao.minPosition(unidadId) ?: 1) - 1
        materialDao.insert(
            MaterialEntity(
                id = "material-${idProvider()}",
                unitId = unidadId,
                title = tituloLimpio,
                type = tipo.name,
                size = archivo?.tamanoLegible ?: tamano?.trim()?.ifBlank { null },
                studied = false,
                downloaded = false,
                description = descripcionLimpia,
                uploadedAt = dateProvider(),
                position = posicion,
                fileName = archivo?.nombre,
                mimeType = archivo?.mimeType
            )
        )

        val fecha = timestampProvider()
        insertarNotificacion(
            rol = RolUsuario.ESTUDIANTE,
            titulo = "Nuevo material disponible",
            descripcion = "$tituloLimpio · ${asignatura.course.name}, Unidad ${unidad.unit.number}",
            tipo = TipoNotificacion.MATERIAL,
            fecha = fecha
        )
    }

    override suspend fun editarMaterial(
        materialId: String,
        titulo: String,
        tipo: TipoMaterial,
        descripcion: String,
        tamano: String?,
        archivo: ArchivoMaterial?
    ) {
        val existente = materialDao.getById(materialId)
            ?: error("Material no encontrado.")
        val tituloLimpio = titulo.trim()
        val descripcionLimpia = descripcion.trim()
        require(tituloLimpio.isNotEmpty() && descripcionLimpia.isNotEmpty()) {
            "El título y la descripción son obligatorios."
        }

        materialDao.update(
            existente.copy(
                title = tituloLimpio,
                type = tipo.name,
                description = descripcionLimpia,
                size = archivo?.tamanoLegible ?: tamano?.trim()?.ifBlank { null },
                fileName = archivo?.nombre ?: existente.fileName,
                mimeType = archivo?.mimeType ?: existente.mimeType
            )
        )
    }

    override suspend fun eliminarMaterial(materialId: String) {
        materialDao.deleteById(materialId)
    }

    override suspend fun marcarNotificacionLeida(notificacionId: String) {
        notificationDao.markRead(notificacionId)
    }

    override suspend fun marcarTodasLeidas(rol: RolUsuario) {
        notificationDao.markAllRead(rol.name)
    }

    private suspend fun insertarNotificacion(
        rol: RolUsuario,
        titulo: String,
        descripcion: String,
        tipo: TipoNotificacion,
        fecha: String
    ) {
        notificationDao.insert(
            NotificationEntity(
                id = "notification-${idProvider()}",
                role = rol.name,
                title = titulo,
                description = descripcion,
                timestamp = fecha,
                read = false,
                type = tipo.name
            )
        )
        notificador.notificar(titulo, descripcion)
    }

    private fun buscarUnidad(asignatura: CourseWithUnits, unidadId: String): UnitWithMaterials =
        asignatura.units.firstOrNull { it.unit.id == unidadId }
            ?: error("Unidad no encontrada.")
}

private fun CourseWithUnits.toDomain(): Asignatura =
    Asignatura(
        id = course.id,
        codigo = course.code,
        nombre = course.name,
        nombreDocente = course.teacherName,
        color = course.color,
        unidades = units
            .sortedBy { it.unit.position }
            .map { unidadConMateriales ->
                Unidad(
                    id = unidadConMateriales.unit.id,
                    numero = unidadConMateriales.unit.number,
                    titulo = unidadConMateriales.unit.title,
                    materiales = unidadConMateriales.materials
                        .sortedBy { it.position }
                        .map { it.toDomain() }
                )
            }
    )

private fun MaterialEntity.toDomain(): Material =
    Material(
        id = id,
        titulo = title,
        tipo = TipoMaterial.valueOf(type),
        tamano = size,
        estudiado = studied,
        descargado = downloaded,
        descripcion = description,
        subidoEl = uploadedAt
    )

private fun MessageEntity.toDomain(): Mensaje =
    Mensaje(
        id = id,
        nombreEstudiante = studentName,
        asignaturaId = courseId,
        nombreAsignatura = courseName,
        unidadId = unitId,
        tituloUnidad = unitTitle,
        materialId = materialId,
        tituloMaterial = materialTitle,
        asunto = subject,
        contenido = content,
        fecha = timestamp,
        respuesta = reply,
        fechaRespuesta = replyTimestamp
    )

private fun NotificationEntity.toDomain(): Notificacion =
    Notificacion(
        id = id,
        rol = RolUsuario.desde(role) ?: error("Rol desconocido: $role"),
        titulo = title,
        descripcion = description,
        fecha = timestamp,
        leida = read,
        tipo = TipoNotificacion.desde(type) ?: error("Tipo de notificación desconocido: $type")
    )
