package de.bavol.passkeyrecovery.controller

import com.webauthn4j.verifier.exception.VerificationException
import de.bavol.passkeyrecovery.exception.CredentialNotFoundException
import de.bavol.passkeyrecovery.exception.LastCredentialException
import de.bavol.passkeyrecovery.exception.PasskeyRecoveryException
import de.bavol.passkeyrecovery.exception.UserNotFoundException
import de.bavol.passkeyrecovery.exception.UsernameTakenException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionHandlers {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(PasskeyRecoveryException::class)
    fun handleDomainException(e: PasskeyRecoveryException): ResponseEntity<String> {
        log.warn("Fachlicher Fehler: {}", e.message)

        return when (e) {
            is UsernameTakenException ->
                ResponseEntity.status(HttpStatus.CONFLICT).body(e.message)

            is LastCredentialException ->
                ResponseEntity.badRequest().body(e.message)

            is CredentialNotFoundException, is UserNotFoundException ->
                ResponseEntity.badRequest().body("Verifikation fehlgeschlagen.")
        }
    }

    @ExceptionHandler(VerificationException::class)
    fun handleVerificationFailure(e: VerificationException): ResponseEntity<String> {
        log.warn("Verifikation fehlgeschlagen: {}", e.message)
        return ResponseEntity.badRequest().body("Verifikation fehlgeschlagen.")
    }
}
