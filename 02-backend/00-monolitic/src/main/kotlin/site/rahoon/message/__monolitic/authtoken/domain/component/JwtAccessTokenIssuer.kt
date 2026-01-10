package site.rahoon.message.__monolitic.authtoken.domain.component

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import site.rahoon.message.__monolitic.common.global.config.JwtProperties
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

data class IssuedAccessToken(
    val token: String,
    val expiresAt: LocalDateTime
)

@Component
class JwtAccessTokenIssuer(
    private val jwtProperties: JwtProperties,
    private val clock: Clock = Clock.systemUTC()
) {
    private val key by lazy {
        val secretBytes = jwtProperties.secret.toByteArray(StandardCharsets.UTF_8)
        require(secretBytes.size >= 32) {
            "jwt.secret must be at least 32 bytes for HS256"
        }
        Keys.hmacShaKeyFor(secretBytes)
    }

    fun issue(userId: String, sessionId: String): IssuedAccessToken {
        val now: Instant = Instant.now(clock)
        val expiresAtInstant = now.plusSeconds(jwtProperties.accessTokenTtlSeconds)
        val expiresAt: LocalDateTime = LocalDateTime.ofInstant(expiresAtInstant, ZoneOffset.UTC)

        val token = Jwts.builder()
            .issuer(jwtProperties.issuer)
            .subject(userId)
            .id(UUID.randomUUID().toString())
            .issuedAt(java.util.Date.from(now))
            .expiration(java.util.Date.from(expiresAtInstant))
            .claim("typ", "access")
            .claim("uid", userId)
            .claim("sid", sessionId)
            .signWith(key, Jwts.SIG.HS256)
            .compact()

        return IssuedAccessToken(
            token = token,
            expiresAt = expiresAt
        )
    }
}

