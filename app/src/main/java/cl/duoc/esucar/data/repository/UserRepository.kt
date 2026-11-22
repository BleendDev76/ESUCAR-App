package cl.duoc.esucar.data.repository

import cl.duoc.esucar.core.security.PasswordHasher
import cl.duoc.esucar.core.time.nowTimestamp
import cl.duoc.esucar.data.local.dao.UserDao
import cl.duoc.esucar.data.local.entity.UserEntity
import cl.duoc.esucar.domain.model.RolUsuario
import java.util.Locale

sealed interface AuthResult {
    data class Success(val user: UserEntity) : AuthResult
    data object DuplicateEmail : AuthResult
    data object InvalidCredentials : AuthResult
}

class UserRepository(
    private val userDao: UserDao,
    private val timestampProvider: () -> String = ::nowTimestamp
) {

    suspend fun registrar(
        nombre: String,
        correo: String,
        clave: String,
        rol: RolUsuario
    ): AuthResult {
        val nombreLimpio = nombre.trim()
        val correoLimpio = normalizarCorreo(correo)
        require(nombreLimpio.isNotEmpty()) { "El nombre es obligatorio." }
        require(correoLimpio.isNotEmpty()) { "El correo es obligatorio." }
        require(clave.length >= 8) {
            "La contraseña debe tener al menos 8 caracteres."
        }

        val claveGuardada = PasswordHasher.hash(clave)
        val usuario = UserEntity(
            name = nombreLimpio,
            email = correoLimpio,
            passwordHash = claveGuardada.hash,
            passwordSalt = claveGuardada.salt,
            role = rol.name,
            createdAt = timestampProvider()
        )

        val id = userDao.insertUser(usuario)
        return if (id == -1L) {
            AuthResult.DuplicateEmail
        } else {
            AuthResult.Success(usuario.copy(id = id.toInt()))
        }
    }

    suspend fun iniciarSesion(correo: String, clave: String): AuthResult {
        val usuario = userDao.getUserByEmail(normalizarCorreo(correo))
            ?: return AuthResult.InvalidCredentials

        return if (PasswordHasher.verify(clave, usuario.passwordHash, usuario.passwordSalt)) {
            AuthResult.Success(usuario)
        } else {
            AuthResult.InvalidCredentials
        }
    }

    private fun normalizarCorreo(correo: String): String =
        correo.trim().lowercase(Locale.ROOT)
}
