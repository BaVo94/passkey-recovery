package de.bavol.passkeyrecovery.controller

import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.client.challenge.Challenge
import de.bavol.passkeyrecovery.service.PasskeyService
import de.bavol.passkeyrecovery.service.UserService
import jakarta.servlet.http.HttpSession
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.security.Principal
import java.util.UUID

@Controller
@RequestMapping("/passkeylist")
class PasskeyController(
    private val passkeyService: PasskeyService,
    private val userService: UserService,
    private val objectConverter: ObjectConverter
) {
    @GetMapping
    fun overview(principal: Principal, model: Model): String {
        val user = userService.getByUsername(principal.name)
        model.addAttribute("user", user)
        model.addAttribute("credentials", passkeyService.listCredentials(user))
        return "passkeylist"
    }

    @PostMapping("/{id}/revoke")
    fun revoke(principal: Principal, @PathVariable id: UUID): String {
        val user = userService.getByUsername(principal.name)
        passkeyService.revokeCredential(user, id)
        return "redirect:/passkeylist"
    }

    @PostMapping("/add/start", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun startAdd(principal: Principal, session: HttpSession): String {
        val user = userService.getByUsername(principal.name)
        val options = passkeyService.startRegistration(user)
        session.setAttribute(AuthController.CHALLENGE_ATTRIBUTE, options.challenge)
        return objectConverter.jsonMapper.writeValueAsString(options)
    }

    @PostMapping("/add/finish")
    @ResponseBody
    fun finishAdd(
        principal: Principal,
        @RequestBody responseJson: String,
        session: HttpSession
    ): ResponseEntity<Void> {
        val challenge = session.getAttribute(AuthController.CHALLENGE_ATTRIBUTE) as? Challenge
            ?: return ResponseEntity.badRequest().build()

        val user = userService.getByUsername(principal.name)
        val registrationData = passkeyService.verifyRegistration(responseJson, challenge)
        passkeyService.storeCredential(user, registrationData)
        session.removeAttribute(AuthController.CHALLENGE_ATTRIBUTE)

        return ResponseEntity.ok().build()
    }

}