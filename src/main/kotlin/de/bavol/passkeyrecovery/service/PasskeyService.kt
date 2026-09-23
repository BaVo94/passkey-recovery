package de.bavol.passkeyrecovery.service

import com.webauthn4j.WebAuthnManager
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.AttestationConveyancePreference
import com.webauthn4j.data.AuthenticatorSelectionCriteria
import com.webauthn4j.data.PublicKeyCredentialCreationOptions
import com.webauthn4j.data.PublicKeyCredentialDescriptor
import com.webauthn4j.data.PublicKeyCredentialParameters
import com.webauthn4j.data.PublicKeyCredentialRpEntity
import com.webauthn4j.data.PublicKeyCredentialType
import com.webauthn4j.data.PublicKeyCredentialUserEntity
import com.webauthn4j.data.RegistrationData
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.data.ResidentKeyRequirement
import com.webauthn4j.data.UserVerificationRequirement
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.Challenge
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import de.bavol.passkeyrecovery.model.PasskeyCredential
import de.bavol.passkeyrecovery.model.User
import de.bavol.passkeyrecovery.repository.PasskeyCredentialRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

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

        return PublicKeyCredentialCreationOptions(
            relyingParty,
            userEntity,
            challenge,
            publicKeyCredentialParams,
            120000L,
            excludedCredentials,
            authenticatorSelection,
            AttestationConveyancePreference.NONE,
            null
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
}