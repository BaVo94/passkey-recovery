package de.bavol.passkeyrecovery.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import java.util.UUID
import java.time.Instant

@Entity
class PasskeyCredential(
    @Id @GeneratedValue val id: UUID? = null,
    @ManyToOne val user: User,
    @Column(unique = true) val credentialId: ByteArray,
    val publicKeyCose: ByteArray,
    var signCount: Long,
    val aaguid: UUID? = null,
    val transports: String? = null,
    val backupEligible: Boolean,
    var backupState: Boolean,
    val createdAt: Instant,
    var lastUsedAt: Instant? = null
)