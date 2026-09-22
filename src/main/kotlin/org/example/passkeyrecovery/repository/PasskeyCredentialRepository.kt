package org.example.passkeyrecovery.repository

import org.example.passkeyrecovery.model.PasskeyCredential
import org.example.passkeyrecovery.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PasskeyCredentialRepository : JpaRepository<PasskeyCredential, UUID> {
    fun findByCredentialId(credentialId: ByteArray): PasskeyCredential?
    fun findAllByUser(user: User): List<PasskeyCredential>
}