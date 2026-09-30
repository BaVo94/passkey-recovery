package de.bavol.passkeyrecovery.service

import de.bavol.passkeyrecovery.exception.DeviceCodeAlreadyUsedException
import de.bavol.passkeyrecovery.exception.DeviceCodeExpiredException
import de.bavol.passkeyrecovery.exception.DeviceCodeUnknownException
import de.bavol.passkeyrecovery.exception.UserNotFoundException
import de.bavol.passkeyrecovery.model.DeviceLoginRequest
import de.bavol.passkeyrecovery.model.DeviceLoginStatus
import de.bavol.passkeyrecovery.model.User
import de.bavol.passkeyrecovery.repository.DeviceLoginRequestRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class DeviceLoginService(
    private val userService: UserService,
    @Value("\${app.recovery.device-code-validity:5m}") private val codeValidity: Duration,
    private val deviceLoginRequestRepository: DeviceLoginRequestRepository,
    private val codeGenerator: CodeGenerator
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun generateCode(): String = codeGenerator.generate(length = 6)

    fun startRequest(username: String): DeviceLoginStartResult {
        val code = generateCode()
        var requestId: UUID? = null

        try {
            val user = userService.getByUsername(username)
            val loginRequest = DeviceLoginRequest(
                user = user,
                codeHash = codeGenerator.hash(code),
                createdAt = Instant.now(),
                expiresAt = Instant.now().plus(codeValidity),
            )
            val saved = deviceLoginRequestRepository.save(loginRequest)
            requestId = saved.id
        } catch (e: UserNotFoundException) {
            log.info("User $username not found")
        }
        return DeviceLoginStartResult(code, requestId)
    }

    fun findPendingByCode(code: String, confirmingUser: User): DeviceLoginRequest {
        val loginRequest =
            deviceLoginRequestRepository.findByCodeHash(codeGenerator.hash(code)) ?: throw DeviceCodeUnknownException()

        if (loginRequest.user.id != confirmingUser.id) {
            log.warn(
                "Gerätecode wurde von Nutzer {} bestätigt, gehört aber zu Nutzer {}",
                confirmingUser.username, loginRequest.user.username
            )
            throw DeviceCodeUnknownException()
        }
        if (Instant.now().isAfter(loginRequest.expiresAt)) {
            throw DeviceCodeExpiredException()
        }
        if (loginRequest.status != DeviceLoginStatus.PENDING) {
            throw DeviceCodeAlreadyUsedException()
        }

        return loginRequest
    }

    @Transactional
    fun approve(code: String, confirmingUser: User) {
        val loginRequest = findPendingByCode(code, confirmingUser)
        loginRequest.status = DeviceLoginStatus.APPROVED
        deviceLoginRequestRepository.save(loginRequest)
    }

    @Transactional
    fun consume(requestId: UUID): User {
        val loginRequest = deviceLoginRequestRepository.findById(requestId)
            .orElseThrow { DeviceCodeUnknownException() }

        if (loginRequest.status != DeviceLoginStatus.APPROVED) {
            throw DeviceCodeAlreadyUsedException()
        }

        loginRequest.status = DeviceLoginStatus.CONSUMED
        deviceLoginRequestRepository.save(loginRequest)
        return loginRequest.user
    }

    fun statusOf(requestId: UUID?, startedAt: Instant): String {
        if (requestId == null) {
            return if (Instant.now().isAfter(startedAt.plus(codeValidity))) "EXPIRED"
            else "PENDING"
        }

        val loginRequest = deviceLoginRequestRepository.findById(requestId).orElse(null)
            ?: return "PENDING"

        if (loginRequest.status == DeviceLoginStatus.PENDING &&
            Instant.now().isAfter(loginRequest.expiresAt)
        ) {
            return "EXPIRED"
        }

        return loginRequest.status.name
    }

    data class DeviceLoginStartResult(val code: String, val requestId: UUID?)

}