package site.rahoon.message.monolithic.core.iam.access.application.service

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.IncorrectClaimException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SecurityException
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.core.iam.access.application.model.AccessTokenResult
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import site.rahoon.message.monolithic.core.iam.exception.AccessError
import site.rahoon.message.monolithic.core.iam.exception.AccessException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID

@ConfigurationProperties(prefix = "core.iam.access-token")
data class CoreAccessTokenProperties(
    val secret: String = "please-change-core-access-token-secret-32bytes",
    val issuer: String = "site.rahoon.message.core",
    val accessTokenTtlSeconds: Long = 3600,
    val refreshTokenTtlSeconds: Long = 1209600,
)

@Service
class CoreAccessTokenService(
    private val properties: CoreAccessTokenProperties,
) {
    private val secretKey = Keys.hmacShaKeyFor(properties.secret.toByteArray())

    /**
     * Issues a signed access token for the given principal.
     */
    fun issue(principal: AuthenticatedPrincipal): AccessTokenResult {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(properties.accessTokenTtlSeconds).truncatedTo(ChronoUnit.SECONDS)
        val token =
            Jwts
                .builder()
                .issuer(properties.issuer)
                .subject(principal.userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .id(UUID.randomUUID().toString())
                .claim("typ", "core-access")
                .claim("uid", principal.userId)
                .claim("sid", principal.sessionId)
                .claim("role", principal.globalRole.name)
                .signWith(secretKey)
                .compact()
        val tokenExpiresAt = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault())
        return AccessTokenResult(
            token = token,
            expiresAt = tokenExpiresAt,
            principal = principal.copy(expiresAt = tokenExpiresAt),
        )
    }

    /**
     * Verifies a signed access token and returns the authenticated principal.
     */
    @Suppress("ThrowsCount")
    fun verify(rawToken: String): AuthenticatedPrincipal {
        val cleanToken = rawToken.trim().removePrefix("Bearer ").removePrefix("bearer ")
        try {
            val claims =
                Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .requireIssuer(properties.issuer)
                    .require("typ", "core-access")
                    .build()
                    .parseSignedClaims(cleanToken)
                    .payload
            val userId = claims.subject ?: throw AccessException(AccessError.INVALID_TOKEN)
            val sessionId = claims.get("sid", String::class.java) ?: throw AccessException(AccessError.INVALID_TOKEN)
            val role = claims.get("role", String::class.java) ?: throw AccessException(AccessError.INVALID_TOKEN)
            val expiresAt = claims.expiration?.toInstant() ?: throw AccessException(AccessError.INVALID_TOKEN)
            return AuthenticatedPrincipal(
                userId = userId,
                sessionId = sessionId,
                globalRole = PrincipalGlobalRole.valueOf(role),
                expiresAt = LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()).truncatedTo(ChronoUnit.SECONDS),
            )
        } catch (e: ExpiredJwtException) {
            throw AccessException(AccessError.TOKEN_EXPIRED, details = mapOf("token" to cleanToken), cause = e)
        } catch (e: MalformedJwtException) {
            throw AccessException(AccessError.INVALID_TOKEN, details = mapOf("token" to cleanToken), cause = e)
        } catch (e: IncorrectClaimException) {
            throw AccessException(AccessError.INVALID_TOKEN, details = mapOf("token" to cleanToken), cause = e)
        } catch (e: SecurityException) {
            throw AccessException(AccessError.INVALID_TOKEN, details = mapOf("token" to cleanToken), cause = e)
        } catch (e: IllegalArgumentException) {
            throw AccessException(AccessError.INVALID_TOKEN, details = mapOf("token" to cleanToken), cause = e)
        }
    }

    /**
     * Returns the configured refresh token TTL in seconds.
     */
    fun refreshTokenTtlSeconds(): Long = properties.refreshTokenTtlSeconds
}
