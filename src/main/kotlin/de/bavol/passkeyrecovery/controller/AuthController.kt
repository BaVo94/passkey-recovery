package de.bavol.passkeyrecovery.controller

import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.client.challenge.Challenge
import com.webauthn4j.verifier.exception.VerificationException
import de.bavol.passkeyrecovery.exception.UsernameTakenException
import de.bavol.passkeyrecovery.model.User
import de.bavol.passkeyrecovery.service.PasskeyService
import de.bavol.passkeyrecovery.service.UserService
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService,
    private val passkeyService: PasskeyService,
    private val objectConverter: ObjectConverter
) {
    @ExceptionHandler(UsernameTakenException::class)
    fun handleUsernameTaken(e: UsernameTakenException) =
        ResponseEntity.status(HttpStatus.CONFLICT).body(e.message)

    @ExceptionHandler(VerificationException::class)
    fun handleVerificationFailure(e: VerificationException) =
        ResponseEntity.badRequest().body("Registrierung konnte nicht verifiziert werden.")

    @GetMapping("/register")
    fun registerPage(): String = "register"

    @PostMapping("/register/start", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun startRegistration(
        @RequestBody request: RegisterStartRequest,
        session: HttpSession
    ): String {
        userService.assertUsernameAvailable(request.username)

        val userHandle = userService.generateUserHandle()
        val pendingUser = User(
            username = request.username,
            displayName = request.displayName,
            userHandle = userHandle
        )
        val options = passkeyService.startRegistration(pendingUser)

        session.setAttribute(CHALLENGE_ATTRIBUTE, options.challenge)
        session.setAttribute(
            PENDING_REGISTRATION_ATTRIBUTE, PendingRegistration(
                request.username, request.displayName, userHandle
            )
        )

        return objectConverter.jsonMapper.writeValueAsString(options)
    }

    @PostMapping("/register/finish")
    @ResponseBody
    fun finishRegistration(
        @RequestBody responseJson: String,
        session: HttpSession
    ): ResponseEntity<Void> {
        val challenge = session.getAttribute(CHALLENGE_ATTRIBUTE) as? Challenge
            ?: return ResponseEntity.badRequest().build()
        val pending = session.getAttribute(PENDING_REGISTRATION_ATTRIBUTE) as? PendingRegistration
            ?: return ResponseEntity.badRequest().build()

        val registrationData = passkeyService.verifyRegistration(responseJson, challenge)
        val user = userService.createUser(pending.username, pending.displayName, pending.userHandle)
        passkeyService.storeCredential(user, registrationData)
        session.removeAttribute(CHALLENGE_ATTRIBUTE)
        session.removeAttribute(PENDING_REGISTRATION_ATTRIBUTE)

        return ResponseEntity.ok().build()
    }

    companion object {
        const val CHALLENGE_ATTRIBUTE = "webauthn.challenge"
        const val PENDING_REGISTRATION_ATTRIBUTE = "pending.registration"
    }

    data class PendingRegistration(
        val username: String,
        val displayName: String,
        val userHandle: ByteArray
    ) : java.io.Serializable
}

data class RegisterStartRequest(val username: String, val displayName: String)