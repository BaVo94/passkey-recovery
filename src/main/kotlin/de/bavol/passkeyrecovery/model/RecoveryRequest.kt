package de.bavol.passkeyrecovery.model

import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import java.time.Instant
import java.util.UUID

@Entity
class RecoveryRequest(
    @Id @GeneratedValue val id: UUID? = null,
    @ManyToOne val user: User,
    @Enumerated(EnumType.STRING) var status: RecoveryStatus = RecoveryStatus.PENDING,
    val createdAt: Instant,
    var processedAt: Instant? = null,
    var codeHash: String? = null,
    var codeExpiresAt: Instant? = null
)

enum class RecoveryStatus { PENDING, APPROVED, REDEEMED, REJECTED }