package de.bavol.passkeyrecovery.repository

import de.bavol.passkeyrecovery.model.RecoveryRequest
import de.bavol.passkeyrecovery.model.RecoveryStatus
import de.bavol.passkeyrecovery.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface RecoveryRequestRepository : JpaRepository<RecoveryRequest, UUID> {
    fun findAllByStatus(status: RecoveryStatus): List<RecoveryRequest>
    fun findByCodeHash(codeHash: String): RecoveryRequest?
    fun findAllByUser(user: User): List<RecoveryRequest>
    fun countByUserAndStatusIn(
        user: User,
        statuses: List<RecoveryStatus>
    ): Long
}