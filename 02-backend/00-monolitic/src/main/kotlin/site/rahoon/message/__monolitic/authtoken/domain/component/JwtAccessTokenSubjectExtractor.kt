package site.rahoon.message.__monolitic.authtoken.domain.component

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.common.global.config.JwtProperties
import java.nio.charset.StandardCharsets

@Component
class JwtAccessTokenSubjectExtractor(
    private val jwtProperties: JwtProperties
) {
    fun extractSubject(accessTokenOrAuthorizationHeader: String): String {
        val rawToken = accessTokenOrAuthorizationHeader
            .trim()
            .removePrefix("Bearer ")
            .trim()

        val key = Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray(StandardCharsets.UTF_8))

        try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(rawToken)
                .payload

            return claims.subject
                ?: throw DomainException(error = AuthTokenError.INVALID_TOKEN)
        } catch (e: ExpiredJwtException) {
            throw DomainException(error = AuthTokenError.TOKEN_EXPIRED)
        } catch (e: Exception) {
            throw DomainException(error = AuthTokenError.INVALID_TOKEN)
        }
    }
}

