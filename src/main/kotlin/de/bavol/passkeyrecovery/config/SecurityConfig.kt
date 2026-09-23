package de.bavol.passkeyrecovery.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.csrf.CookieCsrfTokenRepository

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize("/", permitAll)
                authorize("/error", permitAll)
                authorize("/register", permitAll)
                authorize("/login", permitAll)
                authorize("/auth/**", permitAll)
                authorize("/css/**", permitAll)
                authorize("/js/**", permitAll)
                authorize(anyRequest, authenticated)
            }
            csrf {
                csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse()
            }
            formLogin { disable() }
            httpBasic { disable() }
        }
        return http.build()
    }
}