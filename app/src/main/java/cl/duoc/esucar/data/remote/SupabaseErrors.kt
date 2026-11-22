package cl.duoc.esucar.data.remote

import cl.duoc.esucar.data.remote.dto.ApiErrorDto
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class SupabaseException(
    message: String,
    val statusCode: Int = 0
) : Exception(message)

/**
 * Traduce los errores de Supabase/OkHttp a mensajes que se pueden mostrar en la UI.
 */
object SupabaseErrors {

    private val json = Json { ignoreUnknownKeys = true }

    private val messages = mapOf(
        "invalid_credentials" to "Correo o contraseña incorrectos.",
        "user_already_exists" to "Ese correo ya está registrado.",
        "email_exists" to "Ese correo ya está registrado.",
        "weak_password" to "La contraseña es demasiado débil.",
        "over_email_send_rate_limit" to "Demasiados intentos. Espera un momento.",
        "email_not_confirmed" to "Debes confirmar tu correo antes de entrar."
    )

    /** ¿El error es un 401? Sirve para renovar el token y reintentar una vez. */
    fun esNoAutorizado(throwable: Throwable?): Boolean = when (throwable) {
        is SupabaseException -> throwable.statusCode == 401
        is HttpException -> throwable.code() == 401
        else -> false
    }

    fun userMessage(throwable: Throwable): String = when (throwable) {
        is SupabaseException -> throwable.message ?: "No se pudo completar la operación."
        is UnknownHostException -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
        is SocketTimeoutException -> "El servidor tardó demasiado. Inténtalo otra vez."
        is IOException -> "Sin conexión. Revisa tu red e inténtalo de nuevo."
        is HttpException -> httpMessage(throwable)
        else -> throwable.message ?: "Ocurrió un error inesperado."
    }

    private fun httpMessage(exception: HttpException): String {
        val body = runCatching { exception.response()?.errorBody()?.string() }.getOrNull()
        val parsed = body?.let { raw ->
            runCatching { json.decodeFromString<ApiErrorDto>(raw) }.getOrNull()
        }

        parsed?.errorCode?.let { code -> messages[code]?.let { return it } }

        val serverText = parsed?.msg ?: parsed?.message ?: parsed?.errorDescription
        if (!serverText.isNullOrBlank()) {
            val normalized = serverText.lowercase()
            if ("already registered" in normalized || "already exists" in normalized) {
                return "Ese correo ya está registrado."
            }
            if ("invalid login" in normalized || "invalid credentials" in normalized) {
                return "Correo o contraseña incorrectos."
            }
            return serverText
        }

        return when (exception.code()) {
            401 -> "Sesión expirada. Vuelve a iniciar sesión."
            403 -> "No tienes permisos para realizar esta acción."
            404 -> "El recurso solicitado no existe."
            409 -> "Ese dato ya existe."
            422 -> "Los datos enviados no son válidos."
            in 500..599 -> "El servidor no está disponible. Inténtalo más tarde."
            else -> "No se pudo completar la operación (${exception.code()})."
        }
    }
}
