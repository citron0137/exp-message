package site.rahoon.message.__monolitic.authtoken.domain

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import site.rahoon.message.__monolitic.authtoken.domain.component.JwtAccessTokenIssuer
import site.rahoon.message.__monolitic.authtoken.domain.component.JwtAccessTokenSubjectExtractor
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
        val storage: MutableMap<String, Pair<String, LocalDateTime>> = linkedMapOf() // refreshToken -> (userId, expiresAt)

        override fun saveRefreshToken(userId: String, refreshToken: String, expiresAt: LocalDateTime) {
            storage[refreshToken] = userId to expiresAt
        }

        override fun findUserIdByRefreshToken(refreshToken: String): String? {
            val found = storage[refreshToken] ?: return null
            val (userId, expiresAt) = found
            if (LocalDateTime.now(clock).isAfter(expiresAt)) {
                storage.remove(refreshToken)
                return null
            }
            return userId
        }

        override fun deleteRefreshToken(refreshToken: String) {
            storage.remove(refreshToken)
        }

        override fun deleteAllRefreshTokensByUserId(userId: String) {
            val keys = storage
                .filterValues { (storedUserId, _) -> storedUserId == userId }
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
        val subjectExtractor = JwtAccessTokenSubjectExtractor(jwtProps)
        val service = AuthTokenDomainService(
            accessTokenIssuer = accessTokenIssuer,
            accessTokenSubjectExtractor = subjectExtractor,
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
        val subjectExtractor = JwtAccessTokenSubjectExtractor(jwtProps)
        val service = AuthTokenDomainService(
            accessTokenIssuer = accessTokenIssuer,
            accessTokenSubjectExtractor = subjectExtractor,
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

