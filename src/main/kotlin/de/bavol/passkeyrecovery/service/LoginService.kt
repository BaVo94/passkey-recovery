package de.bavol.passkeyrecovery.service

import de.bavol.passkeyrecovery.model.User
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.stereotype.Service

@Service
class LoginService {
    private val securityContextRepository: SecurityContextRepository = HttpSessionSecurityContextRepository()
    private val log = LoggerFactory.getLogger(javaClass)

    fun authenticate(user: User, request: HttpServletRequest, response: HttpServletResponse) {
        val authentication = UsernamePasswordAuthenticationToken.authenticated(
            user.username,
            null,
            listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
        )

        request.changeSessionId()

        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = authentication
        SecurityContextHolder.setContext(context)
        securityContextRepository.saveContext(context, request, response)
        log.info("Sitzung für Nutzer {} mit Rolle {} erstellt", user.username, user.role)
    }
}