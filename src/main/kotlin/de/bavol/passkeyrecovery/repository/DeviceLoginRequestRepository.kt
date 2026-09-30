package de.bavol.passkeyrecovery.repository

import de.bavol.passkeyrecovery.model.DeviceLoginRequest
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DeviceLoginRequestRepository : JpaRepository<DeviceLoginRequest, UUID> {
    fun findByCodeHash(codeHash: String): DeviceLoginRequest??
}