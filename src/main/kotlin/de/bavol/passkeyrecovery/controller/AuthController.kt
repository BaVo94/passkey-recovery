package de.bavol.passkeyrecovery.controller

import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.client.challenge.Challenge
import de.bavol.passkeyrecovery.model.User
import de.bavol.passkeyrecovery.service.PasskeyService
import de.bavol.passkeyrecovery.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
@RequestMapping("/auth")
class AuthController(
    private val userService: UserService,
    private val passkeyService: PasskeyService,
    private val objectConverter: ObjectConverter,
) {
    private val securityContextRepository: SecurityContextRepository = HttpSessionSecurityContextRepository()

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
        session: HttpSession,
        request: HttpServletRequest,
        response: HttpServletResponse
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

        authenticate(user, request, response)

        return ResponseEntity.ok().build()
    }

    @GetMapping("/login")
    fun loginPage(): String = "login"

    @PostMapping("/login/start", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun startAuthentication(session: HttpSession): String {
        val options = passkeyService.startAuthentication(null)
        session.setAttribute(CHALLENGE_ATTRIBUTE, options.challenge)
        return objectConverter.jsonMapper.writeValueAsString(options)
    }

    @PostMapping("/login/finish")
    @ResponseBody
    fun finishAuthentication(
        @RequestBody responseJson: String,
        session: HttpSession,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val challenge = session.getAttribute(CHALLENGE_ATTRIBUTE) as? Challenge
            ?: return ResponseEntity.badRequest().build()

        val user = passkeyService.finishAuthentication(responseJson, challenge)
        session.removeAttribute(CHALLENGE_ATTRIBUTE)

        authenticate(user, request, response)

        return ResponseEntity.ok().build()
    }

    private fun authenticate(user: User, request: HttpServletRequest, response: HttpServletResponse) {
        val authentication = UsernamePasswordAuthenticationToken.authenticated(
            user.username,
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        request.changeSessionId()

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, request, response)
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