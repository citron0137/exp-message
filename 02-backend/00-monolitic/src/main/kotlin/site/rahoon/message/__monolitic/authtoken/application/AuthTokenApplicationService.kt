package site.rahoon.message.__monolitic.authtoken.application

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.authtoken.domain.AuthToken
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenCommand
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenDomainService
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenRepository
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.common.global.config.JwtProperties
import site.rahoon.message.__monolitic.user.domain.UserRepository
import site.rahoon.message.__monolitic.user.domain.component.UserPasswordHasher
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.LocalDateTime

/**
 * AuthToken Application Service
 * application 레이어에서 User/AuthToken 도메인을 조합해 로그인 플로우를 구성합니다.
 *
 * - email/password 검증: User 도메인(Repository/Hasher)을 사용
 * - 토큰 발급: AuthToken 도메인에 "검증된 userId"만 전달
 */
@Service
class AuthTokenApplicationService(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val authTokenDomainService: AuthTokenDomainService,
    private val authTokenRepository: AuthTokenRepository,
    private val authTokenProperties: AuthTokenProperties,
    private val jwtProperties: JwtProperties,
    private val clock: Clock = Clock.systemUTC()
) {

    @Transactional
    fun login(criteria: AuthTokenCriteria.Login): AuthToken {
        val user = userRepository.findByEmail(criteria.email)
            ?: throw DomainException(
                error = AuthTokenError.INVALID_CREDENTIALS,
                details = mapOf("email" to criteria.email)
            )

        val ok = passwordHasher.verify(criteria.password, user.passwordHash)
        if (!ok) {
            throw DomainException(
                error = AuthTokenError.INVALID_CREDENTIALS,
                details = mapOf("email" to criteria.email)
            )
        }

        val token = authTokenDomainService.issue(
            AuthTokenCommand.Issue(userId = user.id)
        )

        val refreshToken = requireNotNull(token.refreshToken) {
            "refreshToken must not be null for login flow"
        }
        authTokenRepository.saveRefreshToken(
            userId = user.id,
            refreshToken = refreshToken,
            expiresAt = LocalDateTime.now(clock).plusSeconds(authTokenProperties.refreshTokenTtlSeconds)
        )

        return token
    }

    @Transactional
    fun refresh(criteria: AuthTokenCriteria.Refresh): AuthToken {
        val userId = authTokenRepository.findUserIdByRefreshToken(criteria.refreshToken)
            ?: throw DomainException(
                error = AuthTokenError.TOKEN_NOT_FOUND,
                details = mapOf("refreshToken" to criteria.refreshToken)
            )

        // refresh token rotation (기존 토큰은 제거)
        authTokenRepository.deleteRefreshToken(criteria.refreshToken)

        val token = authTokenDomainService.issue(AuthTokenCommand.Issue(userId = userId))
        val newRefreshToken = requireNotNull(token.refreshToken) {
            "refreshToken must not be null for refresh flow"
        }
        authTokenRepository.saveRefreshToken(
            userId = userId,
            refreshToken = newRefreshToken,
            expiresAt = LocalDateTime.now(clock).plusSeconds(authTokenProperties.refreshTokenTtlSeconds)
        )

        return token
    }

    @Transactional
    fun logout(criteria: AuthTokenCriteria.Logout) {
        val userId = extractUserIdFromAccessToken(criteria.accessToken)
        authTokenRepository.deleteAllRefreshTokensByUserId(userId)
    }

    private fun extractUserIdFromAccessToken(accessTokenOrAuthorizationHeader: String): String {
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

