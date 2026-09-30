package de.bavol.passkeyrecovery.controller

import de.bavol.passkeyrecovery.service.DeviceLoginService
import de.bavol.passkeyrecovery.service.LoginService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.time.Instant
import java.util.UUID

@Controller
@RequestMapping("/devicelogin")
class DeviceLoginController(
    private val deviceLoginService: DeviceLoginService,
    private val loginService: LoginService
) {
    @GetMapping
    fun startPage(): String = "devicelogin-start"

    @PostMapping("/start")
    fun start(@RequestParam username: String, session: HttpSession): String {
        val result = deviceLoginService.startRequest(username)

        session.setAttribute(CODE_ATTRIBUTE, result.code)
        session.setAttribute(REQUEST_ID_ATTRIBUTE, result.requestId)
        session.setAttribute(STARTED_AT_ATTRIBUTE, Instant.now())

        return "redirect:/devicelogin/code"
    }

    @GetMapping("/code")
    fun codePage(session: HttpSession, model: Model): String {
        val code = session.getAttribute(CODE_ATTRIBUTE) as? String
            ?: return "redirect:/devicelogin"

        model.addAttribute("code", code)
        return "devicelogin-code"
    }

    @GetMapping("/status", produces = [MediaType.APPLICATION_JSON_VALUE])
    @ResponseBody
    fun status(session: HttpSession): Map<String, String> {
        val requestId = session.getAttribute(REQUEST_ID_ATTRIBUTE) as? UUID
        val startedAt = session.getAttribute(STARTED_AT_ATTRIBUTE) as? Instant
            ?: return mapOf("status" to "EXPIRED")

        return mapOf("status" to deviceLoginService.statusOf(requestId, startedAt))
    }
    
    @PostMapping("/complete")
    @ResponseBody
    fun complete(
        session: HttpSession,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<Void> {
        val requestId = session.getAttribute(REQUEST_ID_ATTRIBUTE) as? UUID
            ?: return ResponseEntity.badRequest().build()

        val user = deviceLoginService.consume(requestId)

        session.removeAttribute(CODE_ATTRIBUTE)
        session.removeAttribute(REQUEST_ID_ATTRIBUTE)

        loginService.authenticate(user, request, response)

        return ResponseEntity.ok().build()
    }

    companion object {
        const val CODE_ATTRIBUTE = "devicelogin.code"
        const val REQUEST_ID_ATTRIBUTE = "devicelogin.requestId"
        const val STARTED_AT_ATTRIBUTE = "devicelogin.startedAt"
    }
}