package de.bavol.passkeyrecovery.controller

import de.bavol.passkeyrecovery.exception.InvalidRecoveryCodeException
import de.bavol.passkeyrecovery.service.LoginService
import de.bavol.passkeyrecovery.service.RecoveryService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/recovery")
class RecoveryController(
    private val recoveryService: RecoveryService,
    private val loginService: LoginService,
) {
    @GetMapping
    fun requestPage(): String = "recovery-request"

    @PostMapping("/request")
    fun submitRequest(@RequestParam username: String): String {
        recoveryService.createRequest(username)
        return "redirect:/recovery/submitted"
    }

    @GetMapping("/submitted")
    fun submittedPage(): String = "recovery-submitted"

    @GetMapping("/redeem")
    fun redeemPage(): String = "recovery-redeem"

    @PostMapping("/redeem")
    fun redeem(
        @RequestParam code: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
        model: Model
    ): String {
        return try {
            val user = recoveryService.redeemRequest(code)
            loginService.authenticate(user, request, response)
            "redirect:/passkeylist"
        } catch (e: InvalidRecoveryCodeException) {
            model.addAttribute("error", "Der eingegebene Code ist ungültig.")
            "recovery-redeem"
        }
    }
}