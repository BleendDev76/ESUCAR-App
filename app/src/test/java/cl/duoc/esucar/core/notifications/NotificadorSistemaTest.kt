package cl.duoc.esucar.core.notifications

import cl.duoc.esucar.data.repository.RoomStudyRepository
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.domain.model.TipoMaterial
import cl.duoc.esucar.domain.model.TipoNotificacion
import cl.duoc.esucar.testing.FakeCourseDao
import cl.duoc.esucar.testing.FakeMaterialDao
import cl.duoc.esucar.testing.FakeMessageDao
import cl.duoc.esucar.testing.FakeNotificationDao
import cl.duoc.esucar.testing.FakeNotificador
import cl.duoc.esucar.testing.consultaDePrueba
import cl.duoc.esucar.testing.cursoDePrueba
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificadorSistemaTest {

    private val notificador = FakeNotificador()
    private val notificationDao = FakeNotificationDao()
    private val courseDao = FakeCourseDao(listOf(cursoDePrueba()))
    private val materialDao = FakeMaterialDao()
    private val messageDao = FakeMessageDao(listOf(consultaDePrueba()))

    private var siguienteId = 0L

    private val repository = RoomStudyRepository(
        courseDao = courseDao,
        materialDao = materialDao,
        messageDao = messageDao,
        notificationDao = notificationDao,
        timestampProvider = { "2026-10-25T10:00:00" },
        dateProvider = { "2026-10-25" },
        idProvider = { ++siguienteId },
        notificador = notificador
    )

    @Test
    fun `enviar consulta avisa al docente y guarda la notificacion`() = runTest {
        repository.enviarConsulta(
            nombreEstudiante = "Ana Pérez",
            asignaturaId = "course-1",
            unidadId = "unit-1",
            materialId = "material-1",
            asunto = "Consulta inicial",
            contenido = "¿Cuándo es la prueba?"
        )

        assertEquals(1, notificador.llamadas.size)
        assertEquals("Nueva consulta de estudiante", notificador.llamadas.single().titulo)
        assertEquals(
            "Consulta inicial · Aplicaciones Móviles",
            notificador.llamadas.single().descripcion
        )
        assertEquals(RolUsuario.DOCENTE.name, notificationDao.notificaciones.single().role)
        assertEquals(TipoNotificacion.CONSULTA.name, notificationDao.notificaciones.single().type)
    }

    @Test
    fun `responder consulta avisa al estudiante`() = runTest {
        repository.responderConsulta("message-1", "La prueba es el viernes.")

        assertEquals(1, notificador.llamadas.size)
        assertEquals("Tu consulta fue respondida", notificador.llamadas.single().titulo)
        assertEquals(
            "Consulta inicial · Aplicaciones Móviles",
            notificador.llamadas.single().descripcion
        )
        assertEquals(RolUsuario.ESTUDIANTE.name, notificationDao.notificaciones.single().role)
        assertEquals(TipoNotificacion.RESPUESTA.name, notificationDao.notificaciones.single().type)
    }

    @Test
    fun `agregar material avisa al estudiante`() = runTest {
        repository.agregarMaterial(
            asignaturaId = "course-1",
            unidadId = "unit-1",
            titulo = "Guía 2",
            tipo = TipoMaterial.GUIA,
            descripcion = "Descripción de la guía.",
            tamano = null,
            archivo = null
        )

        assertEquals(1, notificador.llamadas.size)
        assertEquals("Nuevo material disponible", notificador.llamadas.single().titulo)
        assertEquals(
            "Guía 2 · Aplicaciones Móviles, Unidad 1",
            notificador.llamadas.single().descripcion
        )
        assertEquals(RolUsuario.ESTUDIANTE.name, notificationDao.notificaciones.single().role)
        assertEquals(TipoNotificacion.MATERIAL.name, notificationDao.notificaciones.single().type)
    }
}

