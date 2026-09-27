package de.bavol.passkeyrecovery.controller

import de.bavol.passkeyrecovery.service.RecoveryService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

@Controller
@RequestMapping("/admin")
class AdminController(
    private val recoveryService: RecoveryService
) {
    @GetMapping
    fun adminPage(model: Model): String {

        val pendingRequestViews = recoveryService.listPendingRequests().map { req ->
            RequestView(
                id = req.id!!,
                userName = req.user.username,
                displayName = req.user.displayName,
                createdAt = toLocal(req.createdAt)
            )
        }
        model.addAttribute("requests", pendingRequestViews)

        return "admin"
    }

    @PostMapping("/approve")
    fun approve(
        @RequestParam requestId: UUID,
        redirectAttributes: RedirectAttributes
    ): String {
        val code = recoveryService.approveRequest(requestId)
        redirectAttributes.addFlashAttribute("generatedCode", code)
        return "redirect:/admin"
    }


    @PostMapping("/reject")
    fun reject(
        @RequestParam requestId: UUID,
    ): String {
        recoveryService.rejectRequest(requestId)
        return "redirect:/admin"
    }

    private fun toLocal(instant: Instant): LocalDateTime =
        LocalDateTime.ofInstant(instant, ZoneId.systemDefault())

}

data class RequestView(
    val id: UUID,
    val userName: String,
    val displayName: String,
    val createdAt: LocalDateTime,
)