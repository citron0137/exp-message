package site.rahoon.message.__monolitic.authtoken.domain

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import site.rahoon.message.__monolitic.authtoken.domain.component.JwtAccessTokenIssuer
import site.rahoon.message.__monolitic.authtoken.domain.component.JwtAccessTokenClaimsExtractor
import site.rahoon.message.__monolitic.common.global.config.JwtProperties
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class AuthTokenDomainServiceTest {

    private class InMemoryAuthTokenRepository(
        private val clock: Clock
    ) : AuthTokenRepository {
        val storage: MutableMap<String, Triple<String, String, LocalDateTime>> =
            linkedMapOf() // refreshToken -> (userId, sessionId, expiresAt)

        override fun saveRefreshToken(userId: String, sessionId: String, refreshToken: String, expiresAt: LocalDateTime) {
            storage[refreshToken] = Triple(userId, sessionId, expiresAt)
        }

        override fun findSessionByRefreshToken(refreshToken: String): RefreshTokenSession? {
            val found = storage[refreshToken] ?: return null
            val (userId, sessionId, expiresAt) = found
            if (LocalDateTime.now(clock).isAfter(expiresAt)) {
                storage.remove(refreshToken)
                return null
            }
            return RefreshTokenSession(userId = userId, sessionId = sessionId)
        }

        override fun deleteRefreshToken(refreshToken: String) {
            storage.remove(refreshToken)
        }

        override fun deleteAllRefreshTokensBySessionId(sessionId: String) {
            val keys = storage
                .filterValues { (_, storedSessionId, _) -> storedSessionId == sessionId }
                .keys
            keys.forEach { storage.remove(it) }
        }

        override fun deleteAllRefreshTokensByUserId(userId: String) {
            val keys = storage
                .filterValues { (storedUserId, _, _) -> storedUserId == userId }
                .keys
            keys.forEach { storage.remove(it) }
        }
    }

    @Test
    fun `토큰 발급 성공 - accessToken(JWT)과 refreshToken, expiresAt 조합`() {
        // given
        val fixedNow = Instant.now()
        val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        val jwtProps = JwtProperties(
            secret = "please-change-me-please-change-me-please-change-me-32bytes",
            issuer = "site.rahoon.message.test",
            accessTokenTtlSeconds = 3600
        )
        val authTokenProps = AuthTokenProperties(refreshTokenTtlSeconds = 1209600)
        val repo = InMemoryAuthTokenRepository(clock)
        val accessTokenIssuer = JwtAccessTokenIssuer(jwtProps, clock)
        val claimsExtractor = JwtAccessTokenClaimsExtractor(jwtProps)
        val service = AuthTokenDomainService(
            accessTokenIssuer = accessTokenIssuer,
            accessTokenClaimsExtractor = claimsExtractor,
            authTokenRepository = repo,
            authTokenProperties = authTokenProps,
            clock = clock
        )

        // when
        val token = service.issue(AuthTokenCommand.Issue(userId = "user-123"))

        // then (refreshToken)
        assertNotNull(token.refreshToken)
        assertTrue(token.refreshToken!!.startsWith("refresh.user-123."))
        assertTrue(repo.storage.containsKey(token.refreshToken!!), "발급된 refreshToken은 저장되어야 합니다")

        // then (accessToken 형태)
        assertNotNull(token.accessToken)
        assertTrue(token.accessToken.split('.').size == 3, "JWT는 header.payload.signature 형태여야 합니다")

        // then (expiresAt)
        val expectedExpiresAt = LocalDateTime.ofInstant(fixedNow.plusSeconds(3600), ZoneOffset.UTC)
        assertEquals(expectedExpiresAt, token.expiresAt)

        // then (JWT 클레임: userId가 제대로 들어갔는지)
        val key = Keys.hmacShaKeyFor(jwtProps.secret.toByteArray(StandardCharsets.UTF_8))
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token.accessToken)
            .payload

        assertEquals(jwtProps.issuer, claims.issuer)
        assertEquals("user-123", claims.subject)
        assertEquals("access", claims["typ"])
        assertEquals("user-123", claims["uid"])
        assertNotNull(claims["sid"] as? String, "sid는 반드시 포함되어야 합니다")

        val stored = repo.storage[token.refreshToken!!]
            ?: error("발급된 refreshToken이 저장소에 존재해야 합니다")
        val (_, storedSessionId, _) = stored

        val extracted = claimsExtractor.extract(token.accessToken)
        assertEquals("user-123", extracted.userId)
        assertEquals(storedSessionId, extracted.sessionId, "JWT sid와 저장된 세션 sid는 일치해야 합니다")
    }

    @Test
    fun `secret이 32바이트 미만이면 토큰 발급 실패`() {
        // given
        val fixedNow = Instant.now()
        val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        val jwtProps = JwtProperties(
            secret = "too-short-secret",
            issuer = "site.rahoon.message.test",
            accessTokenTtlSeconds = 3600
        )
        val authTokenProps = AuthTokenProperties(refreshTokenTtlSeconds = 1209600)
        val repo = InMemoryAuthTokenRepository(clock)
        val accessTokenIssuer = JwtAccessTokenIssuer(jwtProps, clock)
        val claimsExtractor = JwtAccessTokenClaimsExtractor(jwtProps)
        val service = AuthTokenDomainService(
            accessTokenIssuer = accessTokenIssuer,
            accessTokenClaimsExtractor = claimsExtractor,
            authTokenRepository = repo,
            authTokenProperties = authTokenProps,
            clock = clock
        )

        // then
        assertThrows(IllegalArgumentException::class.java) {
            service.issue(AuthTokenCommand.Issue(userId = "user-123"))
        }
    }
}

