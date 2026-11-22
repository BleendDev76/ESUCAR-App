package cl.duoc.esucar.data.repository

import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.domain.model.TipoNotificacion
import cl.duoc.esucar.domain.model.cantidadEstudiados
import cl.duoc.esucar.domain.model.cantidadTotal
import cl.duoc.esucar.domain.model.porcentajeProgreso
import cl.duoc.esucar.testing.FakeCourseDao
import cl.duoc.esucar.testing.FakeMaterialDao
import cl.duoc.esucar.testing.FakeMessageDao
import cl.duoc.esucar.testing.FakeNotificationDao
import cl.duoc.esucar.testing.FakeNotificador
import cl.duoc.esucar.testing.consultaDePrueba
import cl.duoc.esucar.testing.cursoDePrueba
import cl.duoc.esucar.testing.materialDePrueba
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomStudyRepositoryTest {

    private val materiales = (1..19).map { indice ->
        materialDePrueba(id = "material-$indice", estudiado = indice <= 7)
    }
    private val notificador = FakeNotificador()
    private val notificationDao = FakeNotificationDao()
    private val materialDao = FakeMaterialDao(materiales)
    private val messageDao = FakeMessageDao(listOf(consultaDePrueba()))
    private var siguienteId = 0L

    private val repository = RoomStudyRepository(
        courseDao = FakeCourseDao(listOf(cursoDePrueba(materiales))),
        materialDao = materialDao,
        messageDao = messageDao,
        notificationDao = notificationDao,
        timestampProvider = { "2026-10-25T10:00:00" },
        dateProvider = { "2026-10-25" },
        idProvider = { ++siguienteId },
        notificador = notificador
    )

    @Test
    fun `progreso de 7 de 19 materiales es 37 por ciento`() = runTest {
        val asignatura = repository.observarAsignaturas().first().single()

        assertEquals(7, asignatura.cantidadEstudiados())
        assertEquals(19, asignatura.cantidadTotal())
        assertEquals(37, asignatura.porcentajeProgreso())
    }

    @Test
    fun `enviar consulta guarda la notificacion del docente y llama al notificador`() = runTest {
        repository.enviarConsulta(
            nombreEstudiante = "Ana Pérez",
            asignaturaId = "course-1",
            unidadId = "unit-1",
            materialId = "material-1",
            asunto = "Consulta inicial",
            contenido = "¿Cuándo es la prueba?"
        )

        val notificacion = notificationDao.notificaciones.single()
        assertEquals(RolUsuario.DOCENTE.name, notificacion.role)
        assertEquals(TipoNotificacion.CONSULTA.name, notificacion.type)
        assertEquals("Nueva consulta de estudiante", notificacion.title)
        assertEquals(1, notificador.llamadas.size)
        assertEquals("Nueva consulta de estudiante", notificador.llamadas.single().titulo)
    }

    @Test
    fun `responder consulta guarda la notificacion del estudiante`() = runTest {
        repository.responderConsulta("message-1", "La prueba es el viernes.")

        val notificacion = notificationDao.notificaciones.single()
        assertEquals(RolUsuario.ESTUDIANTE.name, notificacion.role)
        assertEquals(TipoNotificacion.RESPUESTA.name, notificacion.type)
        assertEquals("Tu consulta fue respondida", notificacion.title)
        assertEquals(1, notificador.llamadas.size)
    }

    @Test
    fun `alternar descarga cambia el flag en los dos sentidos`() = runTest {
        val inicial = repository.obtenerMaterial("material-1")
        assertFalse(inicial?.descargado ?: true)

        repository.alternarDescarga("material-1")
        assertTrue(repository.obtenerMaterial("material-1")?.descargado ?: false)

        repository.alternarDescarga("material-1")
        assertFalse(repository.obtenerMaterial("material-1")?.descargado ?: true)
    }
}
