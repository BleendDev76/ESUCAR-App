package cl.duoc.esucar.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordHash(
    val hash: String,
    val salt: String
)

object PasswordHasher {

    private const val ITERATIONS = 150_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun hash(password: String): PasswordHash {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return PasswordHash(
            hash = derive(password, salt, ITERATIONS).toHex(),
            salt = salt.toHex()
        )
    }

    fun verify(password: String, hash: String, salt: String): Boolean {
        val saltBytes = salt.fromHex() ?: return false
        val expected = hash.fromHex() ?: return false
        val actual = derive(password, saltBytes, ITERATIONS)
        return MessageDigest.isEqual(expected, actual)
    }

    internal fun derive(
        password: String,
        salt: ByteArray,
        iterations: Int,
        keyLengthBits: Int = KEY_LENGTH_BITS
    ): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBits)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            .generateSecret(spec)
            .encoded
    }
}

private fun ByteArray.toHex(): String =
    joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

private fun String.fromHex(): ByteArray? {
    if (length % 2 != 0) return null
    return runCatching {
        ByteArray(length / 2) { index ->
            substring(index * 2, index * 2 + 2).toInt(16).toByte()
        }
    }.getOrNull()
}
