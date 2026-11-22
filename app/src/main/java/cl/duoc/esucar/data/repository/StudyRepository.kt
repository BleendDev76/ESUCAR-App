package cl.duoc.esucar.data.repository

import cl.duoc.esucar.data.remote.ArchivoMaterial
import cl.duoc.esucar.domain.model.Asignatura
import cl.duoc.esucar.domain.model.Material
import cl.duoc.esucar.domain.model.Mensaje
import cl.duoc.esucar.domain.model.Notificacion
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.domain.model.TipoMaterial
import kotlinx.coroutines.flow.Flow

interface StudyRepository {

    fun observarAsignaturas(): Flow<List<Asignatura>>

    fun observarMensajes(): Flow<List<Mensaje>>

    fun observarMensaje(mensajeId: String): Flow<Mensaje?>

    fun observarNotificaciones(rol: RolUsuario): Flow<List<Notificacion>>

    fun observarNoLeidas(rol: RolUsuario): Flow<Int>

    suspend fun obtenerMaterial(materialId: String): Material?

    /** Refresca la caché local desde el backend. Devuelve éxito si no hay backend. */
    suspend fun sincronizar(): Result<Unit> = Result.success(Unit)

    suspend fun marcarEstudiado(materialId: String)

    suspend fun alternarDescarga(materialId: String)

    /** Abre el archivo descargado con el visor del sistema. */
    suspend fun abrirMaterial(materialId: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("Este repositorio no abre archivos."))

    suspend fun enviarConsulta(
        nombreEstudiante: String,
        asignaturaId: String,
        unidadId: String,
        materialId: String?,
        asunto: String,
        contenido: String
    )

    suspend fun responderConsulta(mensajeId: String, respuesta: String)

    suspend fun agregarMaterial(
        asignaturaId: String,
        unidadId: String,
        titulo: String,
        tipo: TipoMaterial,
        descripcion: String,
        tamano: String?,
        archivo: ArchivoMaterial? = null
    )

    suspend fun editarMaterial(
        materialId: String,
        titulo: String,
        tipo: TipoMaterial,
        descripcion: String,
        tamano: String?,
        archivo: ArchivoMaterial? = null
    )

    suspend fun eliminarMaterial(materialId: String)

    suspend fun marcarNotificacionLeida(notificacionId: String)

    suspend fun marcarTodasLeidas(rol: RolUsuario)
}
