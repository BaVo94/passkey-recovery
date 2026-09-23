package de.bavol.passkeyrecovery.controller

import com.webauthn4j.verifier.exception.VerificationException
import de.bavol.passkeyrecovery.exception.CredentialNotFoundException
import de.bavol.passkeyrecovery.exception.LastCredentialException
import de.bavol.passkeyrecovery.exception.UsernameTakenException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class ExceptionHandlers {
    @ExceptionHandler(UsernameTakenException::class)
    fun handleUsernameTaken(e: UsernameTakenException) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(e.message)

    @ExceptionHandler(VerificationException::class)
    fun handleVerificationFailure(e: VerificationException) =
        ResponseEntity.badRequest().body("Verifikation fehlgeschlagen.")

    @ExceptionHandler(CredentialNotFoundException::class)
    fun handleCredentialNotFoundException(e: CredentialNotFoundException) =
        ResponseEntity.badRequest().body("Verifikation fehlgeschlagen.")

    @ExceptionHandler(LastCredentialException::class)
    fun handleLastCredentialException(e: LastCredentialException) =
        ResponseEntity.badRequest().body("Der letzte Passkey eines Kontos kann nicht widerrufen werden.")

}