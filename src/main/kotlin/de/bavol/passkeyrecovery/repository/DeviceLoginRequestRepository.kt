package de.bavol.passkeyrecovery.repository

import de.bavol.passkeyrecovery.model.DeviceLoginRequest
import de.bavol.passkeyrecovery.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface DeviceLoginRequestRepository : JpaRepository<DeviceLoginRequest, UUID> {
    fun findByCodeHash(codeHash: String): DeviceLoginRequest?
    fun findAllByUser(user: User): List<DeviceLoginRequest>
}