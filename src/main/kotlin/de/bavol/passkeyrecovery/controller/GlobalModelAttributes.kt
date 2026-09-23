package de.bavol.passkeyrecovery.controller

import de.bavol.passkeyrecovery.service.PasskeyService
import de.bavol.passkeyrecovery.service.UserService
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute
import java.security.Principal

@ControllerAdvice
class GlobalModelAttributes(
    private val userService: UserService,
    private val passkeyService: PasskeyService
) {

    @ModelAttribute("passkeyCount")
    fun passkeyCount(principal: Principal?): Int {
        if (principal == null) return 0
        val user = userService.findByUsername(principal.name) ?: return 0
        return passkeyService.listCredentials(user).size
    }
}