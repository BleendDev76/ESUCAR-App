package cl.duoc.esucar.data.repository

import cl.duoc.esucar.data.local.dao.UserDao
import cl.duoc.esucar.data.local.entity.UserEntity
import cl.duoc.esucar.domain.model.RolUsuario
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRepositoryTest {

    private val userDao = FakeUserDao()
    private val repository = UserRepository(userDao) { "2026-10-22T10:00:00" }

    @Test
    fun `register stores a hashed password and normalizes email`() = runTest {
        val result = repository.registrar(
            nombre = "  Ana Pérez ",
            correo = "  Ana.Perez@ESUCAR.CL ",
            clave = "clave-segura",
            rol = RolUsuario.ESTUDIANTE
        )

        val user = (result as AuthResult.Success).user
        assertEquals("Ana Pérez", user.name)
        assertEquals("ana.perez@esucar.cl", user.email)
        assertEquals(RolUsuario.ESTUDIANTE.name, user.role)
        assertTrue(user.passwordHash.isNotBlank())
        assertTrue(user.passwordSalt.isNotBlank())
        assertTrue(user.passwordHash != "clave-segura")
    }

    @Test
    fun `register rejects a duplicated email`() = runTest {
        repository.registrar(
            nombre = "Ana Pérez",
            correo = "ana@esucar.cl",
            clave = "clave-segura",
            rol = RolUsuario.ESTUDIANTE
        )

        val result = repository.registrar(
            nombre = "Otra Persona",
            correo = "ANA@esucar.cl",
            clave = "otra-clave",
            rol = RolUsuario.DOCENTE
        )

        assertEquals(AuthResult.DuplicateEmail, result)
    }

    @Test
    fun `login accepts the correct password and rejects a wrong one`() = runTest {
        repository.registrar(
            nombre = "Ana Pérez",
            correo = "ana@esucar.cl",
            clave = "clave-segura",
            rol = RolUsuario.ESTUDIANTE
        )

        val valid = repository.iniciarSesion("ANA@esucar.cl", "clave-segura")
        val invalid = repository.iniciarSesion("ana@esucar.cl", "incorrecta")

        assertTrue(valid is AuthResult.Success)
        assertEquals(AuthResult.InvalidCredentials, invalid)
    }

    @Test
    fun `login rejects an unknown email`() = runTest {
        assertEquals(
            AuthResult.InvalidCredentials,
            repository.iniciarSesion("nadie@esucar.cl", "clave-segura")
        )
    }
}

private class FakeUserDao : UserDao {

    private val users = mutableListOf<UserEntity>()

    override suspend fun insertUser(user: UserEntity): Long {
        if (users.any { it.email == user.email }) return -1L
        val id = (users.maxOfOrNull { it.id } ?: 0) + 1
        users += user.copy(id = id)
        return id.toLong()
    }

    override suspend fun getUserByEmail(email: String): UserEntity? =
        users.firstOrNull { it.email == email }

    override suspend fun getAllUsers(): List<UserEntity> = users.toList()
}
