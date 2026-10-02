package de.bavol.passkeyrecovery.service

import com.webauthn4j.WebAuthnManager
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.credential.CredentialRecordImpl
import com.webauthn4j.data.AttestationConveyancePreference
import com.webauthn4j.data.AuthenticationParameters
import com.webauthn4j.data.AuthenticatorSelectionCriteria
import com.webauthn4j.data.PublicKeyCredentialCreationOptions
import com.webauthn4j.data.PublicKeyCredentialDescriptor
import com.webauthn4j.data.PublicKeyCredentialParameters
import com.webauthn4j.data.PublicKeyCredentialRequestOptions
import com.webauthn4j.data.PublicKeyCredentialRpEntity
import com.webauthn4j.data.PublicKeyCredentialType
import com.webauthn4j.data.PublicKeyCredentialUserEntity
import com.webauthn4j.data.RegistrationData
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.data.ResidentKeyRequirement
import com.webauthn4j.data.UserVerificationRequirement
import com.webauthn4j.data.attestation.authenticator.AAGUID
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData
import com.webauthn4j.data.attestation.authenticator.COSEKey
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.Challenge
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import de.bavol.passkeyrecovery.exception.CredentialNotFoundException
import de.bavol.passkeyrecovery.exception.LastCredentialException
import de.bavol.passkeyrecovery.model.PasskeyCredential
import de.bavol.passkeyrecovery.model.User
import de.bavol.passkeyrecovery.repository.PasskeyCredentialRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID

@Service
class PasskeyService(
    private val credentialRepository: PasskeyCredentialRepository,
    private val webAuthnManager: WebAuthnManager,
    private val objectConverter: ObjectConverter,
    @Value("\${app.webauthn.rp-id}") private val rpId: String,
    @Value("\${app.webauthn.rp-name}") private val rpName: String,
    @Value("\${app.webauthn.origin}") private val origin: String
) {
    private val publicKeyCredentialParams = listOf(
        PublicKeyCredentialParameters(
            PublicKeyCredentialType.PUBLIC_KEY,
            COSEAlgorithmIdentifier.ES256
        ),
        PublicKeyCredentialParameters(
            PublicKeyCredentialType.PUBLIC_KEY,
            COSEAlgorithmIdentifier.RS256
        )
    )

    private val knownAuthenticators = mapOf(
        UUID.fromString("9ddd1817-af5a-4672-a2b9-3e3dd95000a9") to "Windows Hello",
        UUID.fromString("ea9b8d66-4d01-1d21-3ce4-b6b48cb575d4") to "Google Password Manager",
        UUID.fromString("fbfc3007-154e-4ecc-8c0b-6e020557d7bd") to "iCloud-Keychain"
    )

    fun authenticatorName(aaguid: UUID?): String =
        knownAuthenticators[aaguid] ?: "Unbekannter Authenticator"

    fun startRegistration(user: User): PublicKeyCredentialCreationOptions {
        val challenge = DefaultChallenge()

        val relyingParty = PublicKeyCredentialRpEntity(rpId, rpName)

        val userEntity = PublicKeyCredentialUserEntity(
            user.userHandle,
            user.username,
            user.displayName
        )

        val excludedCredentials = if (user.id == null) {
            emptyList()
        } else {
            credentialRepository.findAllByUser(user)
                .map { PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY, it.credentialId, null) }
        }
        val authenticatorSelection = AuthenticatorSelectionCriteria(
            null,
            ResidentKeyRequirement.REQUIRED,
            UserVerificationRequirement.REQUIRED
        )

        val timeout = 120000L
        val attestation = AttestationConveyancePreference.NONE
        val extensions = null

        return PublicKeyCredentialCreationOptions(
            relyingParty,
            userEntity,
            challenge,
            publicKeyCredentialParams,
            timeout,
            excludedCredentials,
            authenticatorSelection,
            attestation,
            extensions
        )
    }

    fun verifyRegistration(responseJson: String, challenge: Challenge): RegistrationData {

        val serverProperty = ServerProperty.builder()
            .origin(Origin(origin))
            .rpId(rpId)
            .challenge(challenge)
            .build()

        val registrationParameters = RegistrationParameters(
            serverProperty,
            publicKeyCredentialParams,
            true
        )

        return webAuthnManager.verifyRegistrationResponseJSON(responseJson, registrationParameters)
    }

    fun storeCredential(user: User, registrationData: RegistrationData): PasskeyCredential {

        val authenticatorData = registrationData.attestationObject?.authenticatorData
            ?: throw IllegalStateException("Registrierungsantwort enthält kein AttestationObject")

        val attestedCredentialData = authenticatorData.attestedCredentialData
            ?: throw IllegalStateException("AuthenticatorData enthält keine AttestedCredentialData")

        val credential = PasskeyCredential(
            user = user,
            credentialId = attestedCredentialData.credentialId,
            publicKeyCose = objectConverter.cborMapper
                .writeValueAsBytes(attestedCredentialData.coseKey),
            signCount = authenticatorData.signCount,
            aaguid = attestedCredentialData.aaguid.value,
            transports = registrationData.transports?.joinToString(",") { it.value },
            backupEligible = authenticatorData.isFlagBE,
            backupState = authenticatorData.isFlagBS,
            createdAt = Instant.now()
        )

        return credentialRepository.save(credential)
    }

    fun startAuthentication(user: User?): PublicKeyCredentialRequestOptions {
        val challenge = DefaultChallenge()

        val allowedCredentials = user?.let {
            credentialRepository.findAllByUser(it)
                .map { cred ->
                    PublicKeyCredentialDescriptor(
                        PublicKeyCredentialType.PUBLIC_KEY,
                        cred.credentialId,
                        null
                    )
                }
        }

        return PublicKeyCredentialRequestOptions(
            challenge,
            120000L,
            rpId,
            allowedCredentials,
            UserVerificationRequirement.REQUIRED,
            null
        )
    }

    fun finishAuthentication(responseJson: String, challenge: Challenge): User {
        val authenticationData = webAuthnManager.parseAuthenticationResponseJSON(responseJson)

        val credentialId = authenticationData.credentialId
            ?: throw IllegalStateException("Authentifizierungsantwort enthält keine Credential-ID")

        val savedCredential = credentialRepository.findByCredentialId(credentialId)
            ?: throw CredentialNotFoundException()

        val userHandle = authenticationData.userHandle
        if (userHandle != null && !userHandle.contentEquals(savedCredential.user.userHandle)) {
            throw IllegalStateException("User Handle passt nicht zum gespeicherten Credential")
        }

        val coseKey = objectConverter.cborMapper.readValue(savedCredential.publicKeyCose, COSEKey::class.java)

        val attestedCredentialData = AttestedCredentialData(
            AAGUID(savedCredential.aaguid),
            savedCredential.credentialId,
            coseKey
        )

        val credentialRecord = CredentialRecordImpl(
            null,                      // attestationStatement
            true,                      // uvInitialized
            savedCredential.backupEligible,
            savedCredential.backupState,
            savedCredential.signCount,
            attestedCredentialData,
            null, null, null, null
        )

        val serverProperty = ServerProperty.builder()
            .origin(Origin(origin))
            .rpId(rpId)
            .challenge(challenge)
            .build()

        val parameters = AuthenticationParameters(
            serverProperty,
            credentialRecord,
            null,      // allowCredentials: alle erlaubt
            true       // userVerificationRequired
        )

        webAuthnManager.verify(authenticationData, parameters)

        val authenticatorData = authenticationData.authenticatorData
            ?: throw IllegalStateException("Authentifizierungsantwort enthält keine AuthenticatorData")

        savedCredential.signCount = authenticatorData.signCount
        savedCredential.backupState = authenticatorData.isFlagBS
        savedCredential.lastUsedAt = Instant.now()
        credentialRepository.save(savedCredential)

        return savedCredential.user
    }

    fun listCredentials(user: User): List<PasskeyCredential> =
        credentialRepository.findAllByUser(user)

    fun revokeCredential(user: User, credentialId: UUID) {
        val credential = credentialRepository.findById(credentialId)
            .orElseThrow { CredentialNotFoundException() }

        if (credential.user.id != user.id) {
            throw CredentialNotFoundException()
        }
        if (credentialRepository.countByUser(user) <= 1) {
            throw LastCredentialException()
        }

        credentialRepository.delete(credential)
    }

    fun revokeAllCredentialsForUser(user: User) {
        for (credential in credentialRepository.findAllByUser(user))
            credentialRepository.delete(credential)
    }

}