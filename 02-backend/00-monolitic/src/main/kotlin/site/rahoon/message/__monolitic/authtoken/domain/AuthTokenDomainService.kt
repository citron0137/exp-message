package site.rahoon.message.__monolitic.authtoken.domain

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.global.config.JwtProperties
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * 인증 토큰 도메인 서비스
 * 토큰 발급, 검증, 갱신 등의 비즈니스 로직을 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class AuthTokenDomainService(
    private val jwtProperties: JwtProperties,
    private val clock: Clock = Clock.systemUTC()
) {

    @Transactional
    fun issue(command: AuthTokenCommand.Issue): AuthToken {
        // NOTE: 사용자 검증/패스워드 검증은 application 레이어에서 수행합니다.
        // 여기서는 "검증된 주체"에 대한 토큰 발급만 책임집니다.
        val now = Instant.now(clock)
        val expiresAtInstant = now.plusSeconds(jwtProperties.accessTokenTtlSeconds)
        val expiresAt = LocalDateTime.ofInstant(expiresAtInstant, ZoneOffset.UTC)

        val secretBytes = jwtProperties.secret.toByteArray(StandardCharsets.UTF_8)
        require(secretBytes.size >= 32) {
            "jwt.secret must be at least 32 bytes for HS256"
        }
        val key = Keys.hmacShaKeyFor(secretBytes)

        val accessToken = Jwts.builder()
            .issuer(jwtProperties.issuer)
            .subject(command.userId)
            .issuedAt(java.util.Date.from(now))
            .expiration(java.util.Date.from(expiresAtInstant))
            .claim("typ", "access")
            .claim("uid", command.userId)
            .signWith(key, Jwts.SIG.HS256)
            .compact()

        val refreshToken = "refresh.${command.userId}.${UUID.randomUUID()}"
        return AuthToken.create(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt
        )
    }

    @Transactional
    fun refresh(command: AuthTokenCommand.Refresh): AuthToken {
        TODO("토큰 갱신 로직 구현")
    }

    @Transactional
    fun logout(command: AuthTokenCommand.Logout) {
        TODO("로그아웃 로직 구현")
    }
}

