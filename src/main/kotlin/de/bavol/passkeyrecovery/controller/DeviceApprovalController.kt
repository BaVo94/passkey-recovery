package de.bavol.passkeyrecovery.controller

import de.bavol.passkeyrecovery.exception.InvalidDeviceCodeException
import de.bavol.passkeyrecovery.service.DeviceLoginService
import de.bavol.passkeyrecovery.service.UserService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.security.Principal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Controller
@RequestMapping("/approve-device")
class DeviceApprovalController(
    private val deviceLoginService: DeviceLoginService,
    private val userService: UserService
) {

    @GetMapping
    fun codeEntryPage(): String = "approve-device"

    @PostMapping
    fun confirmationPage(
        @RequestParam code: String,
        principal: Principal,
        model: Model
    ): String {
        val user = userService.getByUsername(principal.name)

        return try {
            val request = deviceLoginService.findPendingByCode(code, user)

            model.addAttribute("code", code)
            model.addAttribute("username", user.username)
            model.addAttribute("requestedAt", toLocal(request.createdAt))
            "approve-device-confirm"
        } catch (e: InvalidDeviceCodeException) {
            model.addAttribute("error", "Der eingegebene Code ist ungültig.")
            "approve-device"
        }
    }

    @PostMapping("/confirm")
    fun confirm(
        @RequestParam code: String,
        principal: Principal,
        model: Model
    ): String {
        val user = userService.getByUsername(principal.name)

        return try {
            deviceLoginService.approve(code, user)
            "approve-device-done"
        } catch (e: InvalidDeviceCodeException) {
            model.addAttribute("error", "Der eingegebene Code ist ungültig.")
            "approve-device"
        }
    }

    private fun toLocal(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
}