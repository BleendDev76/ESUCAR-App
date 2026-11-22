package cl.duoc.esucar.core.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {

    @Test
    fun `derive matches RFC 6070 vector`() {
        val derived = PasswordHasher.derive(
            password = "password",
            salt = "salt".toByteArray(),
            iterations = 4096,
            keyLengthBits = 160
        )

        assertEquals(
            "4b007901b765489abead49d926f721d065a429c1",
            derived.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        )
    }

    @Test
    fun `hash verifies correct password and rejects wrong one`() {
        val stored = PasswordHasher.hash("clave-segura")

        assertTrue(PasswordHasher.verify("clave-segura", stored.hash, stored.salt))
        assertFalse(PasswordHasher.verify("otra-clave", stored.hash, stored.salt))
    }

    @Test
    fun `same password produces different hashes`() {
        val first = PasswordHasher.hash("clave-segura")
        val second = PasswordHasher.hash("clave-segura")

        assertFalse(first.hash == second.hash)
        assertFalse(first.salt == second.salt)
    }
}
