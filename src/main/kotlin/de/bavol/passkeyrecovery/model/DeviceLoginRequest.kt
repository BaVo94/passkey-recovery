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
class DeviceLoginRequest(
    @Id @GeneratedValue val id: UUID? = null,
    @ManyToOne val user: User,
    val codeHash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    @Enumerated(EnumType.STRING) var status: DeviceLoginStatus = DeviceLoginStatus.PENDING
)

enum class DeviceLoginStatus { PENDING, APPROVED, CONSUMED }