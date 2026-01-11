package site.rahoon.message.__monolitic.authtoken.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.authtoken.domain.component.JwtAccessTokenIssuer
import site.rahoon.message.__monolitic.authtoken.domain.component.JwtAccessTokenClaimsExtractor
import site.rahoon.message.__monolitic.common.domain.DomainException
import java.time.Clock
import java.util.UUID
import java.time.LocalDateTime

/**
 * 인증 토큰 도메인 서비스
 * 토큰 발급, 검증, 갱신 등의 비즈니스 로직을 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class AuthTokenDomainService(
    private val accessTokenIssuer: JwtAccessTokenIssuer,
    private val accessTokenClaimsExtractor: JwtAccessTokenClaimsExtractor,
    private val authTokenRepository: AuthTokenRepository,
    private val authTokenProperties: AuthTokenProperties,
    private val clock: Clock = Clock.systemUTC()
) {

    @Transactional
    fun issue(command: AuthTokenCommand.Issue): AuthToken {
        // NOTE: 사용자 검증/패스워드 검증은 application 레이어에서 수행합니다.
        // 여기서는 "검증된 주체"에 대한 토큰 발급만 책임집니다.
        val sessionId = command.sessionId ?: UUID.randomUUID().toString()
        val issuedAccessToken = accessTokenIssuer.issue(command.userId, sessionId)

        val refreshToken = "refresh.${command.userId}.${UUID.randomUUID()}"
        val token = AuthToken.create(
            accessToken = issuedAccessToken.token,
            refreshToken = refreshToken,
            expiresAt = issuedAccessToken.expiresAt
        )

        authTokenRepository.saveRefreshToken(
            userId = command.userId,
            sessionId = sessionId,
            refreshToken = refreshToken,
            expiresAt = LocalDateTime.now(clock).plusSeconds(authTokenProperties.refreshTokenTtlSeconds)
        )

        return token
    }

    @Transactional
    fun refresh(command: AuthTokenCommand.Refresh): AuthToken {
        val session = authTokenRepository.findSessionByRefreshToken(command.refreshToken)
            ?: throw DomainException(
                error = AuthTokenError.TOKEN_NOT_FOUND,
                details = mapOf("refreshToken" to command.refreshToken)
            )

        // refresh token rotation: 기존 토큰 제거 후 새 토큰 발급
        authTokenRepository.deleteRefreshToken(command.refreshToken)

        return issue(AuthTokenCommand.Issue(userId = session.userId, sessionId = session.sessionId))
    }

    @Transactional
    fun logout(command: AuthTokenCommand.Logout) {
        authTokenRepository.deleteAllRefreshTokensBySessionId(command.sessionId)
    }
}

