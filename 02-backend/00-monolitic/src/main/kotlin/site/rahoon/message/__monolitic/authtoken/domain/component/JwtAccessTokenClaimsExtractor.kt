package site.rahoon.message.__monolitic.authtoken.domain.component

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.common.global.config.JwtProperties
import java.nio.charset.StandardCharsets

data class AccessTokenClaims(
    val userId: String,
    val sessionId: String
)

@Component
class JwtAccessTokenClaimsExtractor(
    private val jwtProperties: JwtProperties
) {
    fun extract(accessTokenOrAuthorizationHeader: String): AccessTokenClaims {
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

            val userId = claims.subject ?: throw DomainException(error = AuthTokenError.INVALID_TOKEN)
            val sessionId = claims["sid"] as? String ?: throw DomainException(error = AuthTokenError.INVALID_TOKEN)

            return AccessTokenClaims(
                userId = userId,
                sessionId = sessionId
            )
        } catch (e: ExpiredJwtException) {
            throw DomainException(error = AuthTokenError.TOKEN_EXPIRED)
        } catch (e: Exception) {
            throw DomainException(error = AuthTokenError.INVALID_TOKEN)
        }
    }
}

