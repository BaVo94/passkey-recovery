package de.bavol.passkeyrecovery.config

import com.webauthn4j.WebAuthnManager
import com.webauthn4j.converter.util.ObjectConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class WebAuthnConfig {

    @Bean
    fun webAuthnManager(): WebAuthnManager {
        return WebAuthnManager.createNonStrictWebAuthnManager()
    }

    @Bean
    fun objectConverter(): ObjectConverter {
        return ObjectConverter()
    }

}