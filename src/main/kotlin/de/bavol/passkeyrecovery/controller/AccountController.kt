package de.bavol.passkeyrecovery.controller

import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.client.challenge.Challenge
import de.bavol.passkeyrecovery.service.PasskeyService
import de.bavol.passkeyrecovery.service.UserService
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.security.Principal

@Controller
@RequestMapping("/account")
class AccountController(
    private val userService: UserService,
    private val passkeyService: PasskeyService,
    private val objectConverter: ObjectConverter
) {

    @GetMapping
    fun accountPage(principal: Principal, model: Model): String {
        model.addAttribute("user", userService.getByUsername(principal.name))
        return "account"
    }

    @PostMapping("/delete/start", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun startDelete(principal: Principal, session: HttpSession): String {
        val user = userService.getByUsername(principal.name)
        val options = passkeyService.startAuthentication(user)

        session.setAttribute(AuthController.CHALLENGE_ATTRIBUTE, options.challenge)
        session.setAttribute(DELETE_INTENT_ATTRIBUTE, true)

        return objectConverter.jsonMapper.writeValueAsString(options)
    }

    @PostMapping("/delete/finish")
    @ResponseBody
    fun finishDelete(
        principal: Principal,
        @RequestBody responseJson: String,
        session: HttpSession
    ): ResponseEntity<Void> {
        val challenge = session.getAttribute(AuthController.CHALLENGE_ATTRIBUTE) as? Challenge
            ?: return ResponseEntity.badRequest().build()
        val intent = session.getAttribute(DELETE_INTENT_ATTRIBUTE) as? Boolean
            ?: return ResponseEntity.badRequest().build()

        val user = userService.getByUsername(principal.name)
        val verifiedUser = passkeyService.finishAuthentication(responseJson, challenge)

        if (verifiedUser.id != user.id) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        userService.deleteUser(user)

        session.invalidate()
        SecurityContextHolder.clearContext()

        return ResponseEntity.ok().build()
    }

    companion object {
        const val DELETE_INTENT_ATTRIBUTE = "account.deleteIntent"
    }
}