package site.rahoon.message.__monolitic.authtoken.domain.component

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.common.global.config.JwtProperties
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class JwtAccessTokenClaimsExtractorTest {

    @Test
    fun `Bearer prefix가 있어도 userId와 sid를 추출한다`() {
        // given
        val fixedNow = Instant.now()
        val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        val jwtProps = JwtProperties(
            secret = "please-change-me-please-change-me-please-change-me-32bytes",
            issuer = "site.rahoon.message.test",
            accessTokenTtlSeconds = 3600
        )
        val issuer = JwtAccessTokenIssuer(jwtProps, clock)
        val extractor = JwtAccessTokenClaimsExtractor(jwtProps)
        val token = issuer.issue(userId = "user-123", sessionId = "sid-abc").token

        // when
        val claims = extractor.extract("Bearer $token")

        // then
        assertEquals("user-123", claims.userId)
        assertEquals("sid-abc", claims.sessionId)
    }

    @Test
    fun `유효하지 않은 토큰이면 INVALID_TOKEN`() {
        // given
        val jwtProps = JwtProperties(
            secret = "please-change-me-please-change-me-please-change-me-32bytes",
            issuer = "site.rahoon.message.test",
            accessTokenTtlSeconds = 3600
        )
        val extractor = JwtAccessTokenClaimsExtractor(jwtProps)

        // then
        val ex = assertThrows(DomainException::class.java) {
            extractor.extract("not-a-jwt")
        }
        assertEquals(AuthTokenError.INVALID_TOKEN, ex.error)
    }

    @Test
    fun `만료된 토큰이면 TOKEN_EXPIRED`() {
        // given
        val fixedNow = Instant.now()
        val clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        val jwtProps = JwtProperties(
            secret = "please-change-me-please-change-me-please-change-me-32bytes",
            issuer = "site.rahoon.message.test",
            accessTokenTtlSeconds = -1 // 이미 만료된 토큰 생성
        )
        val issuer = JwtAccessTokenIssuer(jwtProps, clock)
        val extractor = JwtAccessTokenClaimsExtractor(jwtProps)
        val token = issuer.issue(userId = "user-123", sessionId = "sid-abc").token

        // then
        val ex = assertThrows(DomainException::class.java) {
            extractor.extract(token)
        }
        assertEquals(AuthTokenError.TOKEN_EXPIRED, ex.error)
    }
}

