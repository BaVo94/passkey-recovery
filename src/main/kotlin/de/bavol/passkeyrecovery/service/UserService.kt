package de.bavol.passkeyrecovery.service

import de.bavol.passkeyrecovery.exception.UserNotFoundException
import de.bavol.passkeyrecovery.exception.UsernameTakenException
import de.bavol.passkeyrecovery.model.User
import de.bavol.passkeyrecovery.repository.PasskeyCredentialRepository
import de.bavol.passkeyrecovery.repository.RecoveryRequestRepository
import de.bavol.passkeyrecovery.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val credentialRepository: PasskeyCredentialRepository,
    private val recoveryRequestRepository: RecoveryRequestRepository
) {
    private val secureRandom = SecureRandom()

    fun generateUserHandle(): ByteArray {
        val handle = ByteArray(32)
        secureRandom.nextBytes(handle)
        return handle
    }

    fun createUser(username: String, displayName: String, userHandle: ByteArray): User {
        assertUsernameAvailable(username)
        return userRepository.save(
            User(
                username = username,
                displayName = displayName,
                userHandle = userHandle
            )
        )
    }

    fun assertUsernameAvailable(username: String) {
        if (userRepository.findByUsername(username) != null) {
            throw UsernameTakenException(username)
        }
    }


    fun findByUsername(username: String): User? {
        return userRepository.findByUsername(username)
    }

    fun getById(id: UUID): User {
        return userRepository.findById(id)
            .orElseThrow { UserNotFoundException(id) }
    }

    fun getByUsername(username: String): User {
        return userRepository.findByUsername(username) ?: throw UserNotFoundException(username)
    }

    @Transactional
    fun deleteUser(user: User) {
        credentialRepository.deleteAll(credentialRepository.findAllByUser(user))
        recoveryRequestRepository.deleteAll(recoveryRequestRepository.findAllByUser(user))
        userRepository.delete(user)
    }
}
