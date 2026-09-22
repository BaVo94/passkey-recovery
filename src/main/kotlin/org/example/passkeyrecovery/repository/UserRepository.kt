package org.example.passkeyrecovery.repository

import org.example.passkeyrecovery.model.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByUsername(username: String): User?
}