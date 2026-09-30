package de.bavol.passkeyrecovery.service

import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

@Component
class CodeGenerator {

    private val secureRandom = SecureRandom()
    private val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    fun generate(length: Int, groupSize: Int? = null): String {
        val code = buildString {
            repeat(length) { append(alphabet[secureRandom.nextInt(alphabet.length)]) }
        }
        return if (groupSize == null) code else code.chunked(groupSize).joinToString("-")
    }

    fun hash(code: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(normalize(code).toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun normalize(code: String): String =
        code.uppercase().filter { it.isLetterOrDigit() }
}
