package de.bavol.passkeyrecovery.service

import de.bavol.passkeyrecovery.exception.InvalidRecoveryRequestStateException
import de.bavol.passkeyrecovery.exception.RecoveryCodeExpiredException
import de.bavol.passkeyrecovery.exception.RecoveryCodeNotApprovedException
import de.bavol.passkeyrecovery.exception.RecoveryCodeUnknownException
import de.bavol.passkeyrecovery.exception.RecoveryRequestNotFoundException
import de.bavol.passkeyrecovery.exception.UserNotFoundException
import de.bavol.passkeyrecovery.model.RecoveryRequest
import de.bavol.passkeyrecovery.model.RecoveryStatus
import de.bavol.passkeyrecovery.model.User
import de.bavol.passkeyrecovery.repository.RecoveryRequestRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.Instant
import java.util.*


@Service
class RecoveryService(
    private val userService: UserService,
    private val recoveryRequestRepository: RecoveryRequestRepository,
    private val passkeyService: PasskeyService,
    private val codeGenerator: CodeGenerator,
    @Value("\${app.recovery.code-validity:7d}") private val codeValidity: Duration
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private fun generateCode(): String = codeGenerator.generate(length = 12, groupSize = 4)

    fun createRequest(username: String) {
        try {
            val user = userService.getByUsername(username)
            if (recoveryRequestRepository.countByUserAndStatusIn(
                    user,
                    listOf(RecoveryStatus.PENDING, RecoveryStatus.APPROVED)
                ) > 0
            ) {
                log.warn("Offene Wiederherstellungsanfrage für {} existiert bereits", username)
            } else {
                val recoveryRequest = RecoveryRequest(
                    user = user,
                    createdAt = Instant.now(),
                )
                recoveryRequestRepository.save(recoveryRequest)
            }
        } catch (e: UserNotFoundException) {
            log.warn("User $username not found")
        }
    }

    fun listPendingRequests(): List<RecoveryRequest> {
        return recoveryRequestRepository.findAllByStatus(RecoveryStatus.PENDING)
    }

    @Transactional
    fun approveRequest(requestId: UUID): String {
        val pendingRequest =
            recoveryRequestRepository.findById(requestId).orElseThrow { RecoveryRequestNotFoundException(requestId) }

        if (pendingRequest.status !== RecoveryStatus.PENDING) throw InvalidRecoveryRequestStateException(
            requestId,
            pendingRequest.status
        )

        val code = generateCode()

        passkeyService.revokeAllCredentialsForUser(pendingRequest.user)

        pendingRequest.processedAt = Instant.now()
        pendingRequest.codeExpiresAt = Instant.now().plus(codeValidity)
        pendingRequest.codeHash = codeGenerator.hash(code)
        pendingRequest.status = RecoveryStatus.APPROVED
        recoveryRequestRepository.save(pendingRequest)

        return code

    }

    @Transactional
    fun rejectRequest(requestId: UUID) {
        val pendingRequest =
            recoveryRequestRepository.findById(requestId).orElseThrow { RecoveryRequestNotFoundException(requestId) }

        if (pendingRequest.status !== RecoveryStatus.PENDING) throw InvalidRecoveryRequestStateException(
            requestId,
            pendingRequest.status
        )

        pendingRequest.processedAt = Instant.now()
        pendingRequest.status = RecoveryStatus.REJECTED
        recoveryRequestRepository.save(pendingRequest)
    }


    @Transactional
    fun redeemRequest(code: String): User {
        val request = recoveryRequestRepository.findByCodeHash(codeGenerator.hash(code))
            ?: throw RecoveryCodeUnknownException()
        val expiresAt = request.codeExpiresAt ?: throw RecoveryCodeUnknownException()
        if (Instant.now().isAfter(expiresAt)) throw RecoveryCodeExpiredException(request.id)
        else if (request.status !== RecoveryStatus.APPROVED) throw RecoveryCodeNotApprovedException(
            request.id,
            request.status
        )
        else {
            request.status = RecoveryStatus.REDEEMED
            recoveryRequestRepository.save(request)
            return request.user
        }

    }
}