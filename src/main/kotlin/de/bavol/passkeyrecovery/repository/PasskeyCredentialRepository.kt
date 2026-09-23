package de.bavol.passkeyrecovery.repository

import de.bavol.passkeyrecovery.model.PasskeyCredential
import de.bavol.passkeyrecovery.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PasskeyCredentialRepository : JpaRepository<PasskeyCredential, UUID> {
    fun findByCredentialId(credentialId: ByteArray): PasskeyCredential?
    fun findAllByUser(user: User): List<PasskeyCredential>
    fun countByUser(user: User): Long
}