package cl.duoc.esucar.data.remote

import cl.duoc.esucar.data.prefs.SessionStore
import cl.duoc.esucar.data.remote.api.SupabaseAuthApi
import cl.duoc.esucar.data.remote.api.SupabaseRestApi
import cl.duoc.esucar.data.remote.dto.AuthResponse
import cl.duoc.esucar.data.remote.dto.LoginRequest
import cl.duoc.esucar.data.remote.dto.RefreshRequest
import cl.duoc.esucar.data.remote.dto.SignUpRequest
import cl.duoc.esucar.data.remote.dto.UserMetadata
import cl.duoc.esucar.domain.model.RolUsuario
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

data class SessionInfo(
    val userId: String,
    val email: String,
    val name: String,
    val rol: RolUsuario
)

/**
 * Autenticación contra Supabase Auth.
 *
 * El rol autoritativo se lee de `profiles` después del login: el `user_metadata`
 * lo controla el cliente y no sirve como control de acceso.
 */
class SupabaseAuthRepository(
    private val authApi: SupabaseAuthApi,
    private val restApi: SupabaseRestApi,
    private val sessionStore: SessionStore
) {

    suspend fun registrar(
        nombre: String,
        email: String,
        password: String,
        rol: RolUsuario,
        teacherCode: String?
    ): Result<SessionInfo> = capture {
        val normalizedEmail = email.trim().lowercase()
        val response = authApi.signUp(
            SignUpRequest(
                email = normalizedEmail,
                password = password,
                data = UserMetadata(
                    fullName = nombre.trim(),
                    role = rol.name,
                    teacherCode = teacherCode?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        )
        persistSession(response, fallbackName = nombre.trim())
    }

    suspend fun iniciarSesion(email: String, password: String): Result<SessionInfo> = capture {
        val response = authApi.login(
            LoginRequest(
                email = email.trim().lowercase(),
                password = password
            )
        )
        persistSession(response, fallbackName = "")
    }

    /**
     * Renueva el access token con el refresh token guardado.
     * Devuelve true si la sesión sigue siendo válida.
     */
    suspend fun refrescarSesion(): Boolean {
        val refreshToken = sessionStore.cachedRefreshToken ?: return false
        return try {
            val response = authApi.refresh(RefreshRequest(refreshToken))
            val access = response.effectiveAccessToken ?: return false
            val refresh = response.effectiveRefreshToken ?: refreshToken
            sessionStore.updateTokens(access, refresh)
            true
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            if (error is HttpException && error.code() in listOf(400, 401)) {
                sessionStore.clear()
            }
            false
        }
    }

    suspend fun cerrarSesion() {
        runCatching { authApi.logout() }
        sessionStore.clear()
    }

    suspend fun perfilActual(): Result<SessionInfo> = capture {
        val userId = sessionStore.cachedUserId
            ?: throw SupabaseException("No hay una sesión activa.")
        val profile = restApi.profile("eq.$userId").firstOrNull()
            ?: throw SupabaseException("No se encontró el perfil del usuario.")
        val rol = RolUsuario.desde(profile.role)
            ?: throw SupabaseException("El perfil tiene un rol no válido.")
        SessionInfo(
            userId = profile.id,
            email = "",
            name = profile.fullName,
            rol = rol
        )
    }

    private suspend fun persistSession(
        response: AuthResponse,
        fallbackName: String
    ): SessionInfo {
        val access = response.effectiveAccessToken
        val refresh = response.effectiveRefreshToken
        val user = response.effectiveUser

        if (access.isNullOrBlank() || refresh.isNullOrBlank() || user == null) {
            throw SupabaseException(
                "Cuenta creada, pero falta confirmar el correo. Revisa tu bandeja antes de entrar."
            )
        }

        val email = user.email.orEmpty()
        val metadataName = user.userMetadata?.fullName?.takeIf { it.isNotBlank() } ?: fallbackName

        // Guardamos credenciales sin rol para autenticar la consulta, sin abrir una pestaña.
        sessionStore.setSupabaseCredentials(
            accessToken = access,
            refreshToken = refresh,
            userId = user.id
        )

        val profile = try {
            restApi.profile("eq.${user.id}").firstOrNull()
                ?: throw SupabaseException("No se encontró el perfil del usuario.")
        } catch (error: Exception) {
            sessionStore.clear()
            throw error
        }
        val realRol = RolUsuario.desde(profile.role) ?: run {
            sessionStore.clear()
            throw SupabaseException("El perfil tiene un rol no válido.")
        }
        val realName = profile.fullName.takeIf { it.isNotBlank() } ?: metadataName.ifBlank { fallbackName }

        sessionStore.setSupabaseSession(
            accessToken = access,
            refreshToken = refresh,
            userId = user.id,
            email = email,
            name = realName.ifBlank { "Usuario ESUCAR" },
            rol = realRol
        )

        return SessionInfo(
            userId = user.id,
            email = email,
            name = realName.ifBlank { "Usuario ESUCAR" },
            rol = realRol
        )
    }

    private suspend inline fun <T> capture(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        Result.failure(
            SupabaseException(
                SupabaseErrors.userMessage(error),
                (error as? HttpException)?.code() ?: 0
            )
        )
    }
}
