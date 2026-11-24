package cl.duoc.esucar.data.repository

import cl.duoc.esucar.data.prefs.SessionStore
import cl.duoc.esucar.data.remote.ArchivoMaterial
import cl.duoc.esucar.data.remote.SupabaseAuthRepository
import cl.duoc.esucar.data.remote.SupabaseErrors
import cl.duoc.esucar.data.remote.SupabaseException
import cl.duoc.esucar.data.remote.SupabaseSyncRepository
import cl.duoc.esucar.domain.model.Asignatura
import cl.duoc.esucar.domain.model.Material
import cl.duoc.esucar.domain.model.Mensaje
import cl.duoc.esucar.domain.model.Notificacion
import cl.duoc.esucar.domain.model.RolUsuario
import cl.duoc.esucar.domain.model.TipoMaterial
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de la app cuando Supabase está configurado.
 *
 * - Las **lecturas** salen siempre de Room: la UI es reactiva y sigue
 *   funcionando sin conexión con la última copia descargada.
 * - Las **escrituras** van al backend (que aplica RLS) y luego refrescan Room.
 * - Sin sesión remota (modo demostración) todo se delega al repositorio local.
 */
class SupabaseStudyRepository(
    private val local: RoomStudyRepository,
    private val sync: SupabaseSyncRepository,
    private val sessionStore: SessionStore,
    private val auth: SupabaseAuthRepository? = null
) : StudyRepository {

    private val haySesionRemota: Boolean
        get() = !sessionStore.cachedAccessToken.isNullOrBlank()

    override fun observarAsignaturas(): Flow<List<Asignatura>> = local.observarAsignaturas()

    override fun observarMensajes(): Flow<List<Mensaje>> = local.observarMensajes()

    override fun observarMensaje(mensajeId: String): Flow<Mensaje?> =
        local.observarMensaje(mensajeId)

    override fun observarNotificaciones(rol: RolUsuario): Flow<List<Notificacion>> =
        local.observarNotificaciones(rol)

    override fun observarNoLeidas(rol: RolUsuario): Flow<Int> = local.observarNoLeidas(rol)

    override suspend fun obtenerMaterial(materialId: String): Material? =
        local.obtenerMaterial(materialId)

    /**
     * Sincroniza con Supabase y, si el access token venció, lo renueva una sola
     * vez antes de reintentar. Si el refresh token tampoco sirve, `SessionStore`
     * borra la sesión y la app vuelve sola al login.
     */
    override suspend fun sincronizar(): Result<Unit> {
        if (!haySesionRemota) return Result.success(Unit)

        val primero = sync.sincronizar()
        val error = primero.exceptionOrNull()
        if (primero.isSuccess || !SupabaseErrors.esNoAutorizado(error)) return primero

        val renovada = auth?.refrescarSesion() == true
        return if (renovada) sync.sincronizar() else primero
    }

    override suspend fun marcarEstudiado(materialId: String) {
        if (!haySesionRemota) {
            local.marcarEstudiado(materialId)
            return
        }
        // Se refleja de inmediato en la barra de progreso y luego se sincroniza.
        runCatching { sync.marcarEstudiado(materialId) }
    }

    override suspend fun alternarDescarga(materialId: String) {
        if (!haySesionRemota) {
            local.alternarDescarga(materialId)
            return
        }
        val material = local.obtenerMaterial(materialId) ?: return
        if (material.descargado) {
            remoto { sync.quitarDescarga(materialId) }
        } else {
            remoto { sync.descargarMaterial(materialId).getOrThrow() }
        }
    }

    override suspend fun abrirMaterial(materialId: String): Result<Unit> =
        if (haySesionRemota) {
            sync.abrirMaterial(materialId)
        } else {
            Result.failure(IllegalStateException("Conecta la app a Supabase para abrir archivos."))
        }

    override suspend fun enviarConsulta(
        nombreEstudiante: String,
        asignaturaId: String,
        unidadId: String,
        materialId: String?,
        asunto: String,
        contenido: String
    ) {
        if (!haySesionRemota) {
            local.enviarConsulta(
                nombreEstudiante = nombreEstudiante,
                asignaturaId = asignaturaId,
                unidadId = unidadId,
                materialId = materialId,
                asunto = asunto,
                contenido = contenido
            )
            return
        }
        remoto {
            sync.enviarConsulta(
                nombreEstudiante,
                asignaturaId,
                unidadId,
                materialId,
                asunto,
                contenido
            )
        }
    }

    override suspend fun responderConsulta(mensajeId: String, respuesta: String) {
        if (!haySesionRemota) {
            local.responderConsulta(mensajeId, respuesta)
            return
        }
        remoto { sync.responderConsulta(mensajeId, respuesta) }
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
        if (!haySesionRemota) {
            local.agregarMaterial(
                asignaturaId = asignaturaId,
                unidadId = unidadId,
                titulo = titulo,
                tipo = tipo,
                descripcion = descripcion,
                tamano = tamano,
                archivo = archivo
            )
            return
        }
        remoto { sync.agregarMaterial(unidadId, titulo, tipo, descripcion, archivo) }
    }

    override suspend fun editarMaterial(
        materialId: String,
        titulo: String,
        tipo: TipoMaterial,
        descripcion: String,
        tamano: String?,
        archivo: ArchivoMaterial?
    ) {
        if (!haySesionRemota) {
            local.editarMaterial(materialId, titulo, tipo, descripcion, tamano, archivo)
            return
        }
        remoto { sync.editarMaterial(materialId, titulo, tipo, descripcion, archivo) }
    }

    override suspend fun eliminarMaterial(materialId: String) {
        if (!haySesionRemota) {
            local.eliminarMaterial(materialId)
            return
        }
        remoto { sync.eliminarMaterial(materialId) }
    }

    override suspend fun marcarNotificacionLeida(notificacionId: String) {
        local.marcarNotificacionLeida(notificacionId)
        if (haySesionRemota) runCatching { sync.marcarNotificacionLeida(notificacionId) }
    }

    override suspend fun marcarTodasLeidas(rol: RolUsuario) {
        local.marcarTodasLeidas(rol)
        if (haySesionRemota) runCatching { sync.marcarTodasLeidas(rol) }
    }

    /** Traduce los errores HTTP/IO de Supabase a mensajes que la UI puede mostrar. */
    private suspend fun <T> remoto(bloque: suspend () -> T): T = try {
        bloque()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        throw SupabaseException(SupabaseErrors.userMessage(error))
    }
}
