package de.bavol.passkeyrecovery.controller

import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class HomeController {

    @GetMapping("/")
    fun index(authentication: Authentication?): String {
        return if (authentication != null && authentication.isAuthenticated) {
            "redirect:/passkeylist"
        } else {
            "index"
        }
    }
}