package site.rahoon.message.__monolitic.authtoken.domain.component

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import site.rahoon.message.__monolitic.common.global.config.JwtProperties
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class JwtAccessTokenIssuerTest {

    @Test
    fun `JWT accessToken 발급 성공 - 표준 클레임 및 커스텀 클레임 포함`() {
        // given
        val fixedNow = Instant.now()
        val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        val props = JwtProperties(
            secret = "please-change-me-please-change-me-please-change-me-32bytes",
            issuer = "site.rahoon.message.test",
            accessTokenTtlSeconds = 3600
        )
        val issuer = JwtAccessTokenIssuer(props, clock)

        // when
        val issued = issuer.issue(userId = "user-123")

        // then (형태/만료)
        assertNotNull(issued.token)
        assertTrue(issued.token.split('.').size == 3, "JWT는 header.payload.signature 형태여야 합니다")

        val expectedExpiresAt = LocalDateTime.ofInstant(fixedNow.plusSeconds(3600), ZoneOffset.UTC)
        assertEquals(expectedExpiresAt, issued.expiresAt)

        // then (서명 검증 + 클레임 검증)
        val key = Keys.hmacShaKeyFor(props.secret.toByteArray(StandardCharsets.UTF_8))
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(issued.token)
            .payload

        assertEquals(props.issuer, claims.issuer)
        assertEquals("user-123", claims.subject)
        assertEquals(fixedNow.epochSecond, claims.issuedAt.toInstant().epochSecond)
        assertEquals(fixedNow.plusSeconds(3600).epochSecond, claims.expiration.toInstant().epochSecond)
        assertEquals("access", claims["typ"])
        assertEquals("user-123", claims["uid"])
    }

    @Test
    fun `jwt secret이 32바이트 미만이면 예외`() {
        // given
        val clock = Clock.fixed(Instant.parse("2026-01-10T00:00:00Z"), ZoneOffset.UTC)
        val props = JwtProperties(
            secret = "too-short-secret",
            issuer = "site.rahoon.message.test",
            accessTokenTtlSeconds = 3600
        )
        val issuer = JwtAccessTokenIssuer(props, clock)

        // then
        assertThrows(IllegalArgumentException::class.java) {
            issuer.issue(userId = "user-123")
        }
    }
}

