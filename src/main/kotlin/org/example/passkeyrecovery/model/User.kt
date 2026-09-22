package org.example.passkeyrecovery.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "users")
class User(
    @Id @GeneratedValue val id: UUID? = null,
    @Column(unique = true) val username: String,
    val displayName: String,
    @Column(unique = true) val userHandle: ByteArray
)