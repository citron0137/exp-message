package site.rahoon.message.monolithic.core.iam.access.application.facade

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthSessionResult
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.port.AccessPasswordVerifier
import site.rahoon.message.monolithic.core.iam.access.application.port.CoreRefreshTokenRepository
import site.rahoon.message.monolithic.core.iam.access.application.port.LoginPrincipalReader
import site.rahoon.message.monolithic.core.iam.access.application.service.CoreAccessTokenService
import site.rahoon.message.monolithic.core.iam.access.domain.CoreRefreshToken
import site.rahoon.message.monolithic.core.iam.exception.AccessError
import site.rahoon.message.monolithic.core.iam.exception.AccessException
import java.time.LocalDateTime
import java.util.UUID

@Service
class AccessFacade(
    private val loginPrincipalReader: LoginPrincipalReader,
    private val passwordVerifier: AccessPasswordVerifier,
    private val refreshTokenRepository: CoreRefreshTokenRepository,
    private val accessTokenService: CoreAccessTokenService,
) {
    /**
     * Logs in a principal with email and password.
     */
    @Transactional
    fun login(command: LoginCommand): AuthSessionResult {
        val loginPrincipal =
            loginPrincipalReader.findByEmail(command.email)
                ?: throw AccessException(AccessError.INVALID_CREDENTIALS)
        if (!passwordVerifier.verify(command.password, loginPrincipal.passwordHash)) {
            throw AccessException(AccessError.INVALID_CREDENTIALS)
        }
        val sessionId = UUID.randomUUID().toString()
        val principal =
            AuthenticatedPrincipal(
                userId = loginPrincipal.userId,
                sessionId = sessionId,
                globalRole = loginPrincipal.globalRole,
                expiresAt = LocalDateTime.now(),
            )
        return issueSession(principal)
    }

    /**
     * Refreshes an authenticated session from a refresh token.
     */
    @Transactional
    fun refresh(command: RefreshCommand): AuthSessionResult {
        val existing =
            refreshTokenRepository.findByToken(command.refreshToken)
                ?: throw AccessException(AccessError.REFRESH_TOKEN_NOT_FOUND)
        if (existing.isExpired(LocalDateTime.now())) {
            refreshTokenRepository.deleteByToken(existing.token)
            throw AccessException(AccessError.REFRESH_TOKEN_EXPIRED)
        }
        refreshTokenRepository.deleteByToken(existing.token)
        val loginPrincipal =
            loginPrincipalReader.findById(existing.userId)
                ?: throw AccessException(
                    error = AccessError.INVALID_TOKEN,
                    details = mapOf("reason" to "Cannot resolve user for refresh token"),
                )
        val principal =
            AuthenticatedPrincipal(
                userId = existing.userId,
                sessionId = UUID.randomUUID().toString(),
                globalRole = loginPrincipal.globalRole,
                expiresAt = LocalDateTime.now(),
            )
        return issueSession(principal)
    }

    /**
     * Logs out an authenticated principal by revoking its refresh session.
     */
    @Transactional
    fun logout(principal: AuthenticatedPrincipal) {
        refreshTokenRepository.deleteBySessionId(principal.sessionId)
    }

    /**
     * Verifies a raw bearer token and returns the authenticated principal.
     */
    fun verifyAccessToken(rawToken: String): AuthenticatedPrincipal = accessTokenService.verify(rawToken)

    /**
     * Issues access and refresh tokens for a principal.
     */
    private fun issueSession(principal: AuthenticatedPrincipal): AuthSessionResult {
        val accessToken = accessTokenService.issue(principal)
        val now = LocalDateTime.now()
        val refreshToken =
            CoreRefreshToken(
                token = UUID.randomUUID().toString(),
                userId = principal.userId,
                sessionId = accessToken.principal.sessionId,
                expiresAt = now.plusSeconds(accessTokenService.refreshTokenTtlSeconds()),
                createdAt = now,
            )
        val savedRefreshToken = refreshTokenRepository.save(refreshToken)
        return AuthSessionResult(
            accessToken = accessToken,
            refreshToken = savedRefreshToken.token,
            refreshTokenExpiresAt = savedRefreshToken.expiresAt,
        )
    }
}

data class LoginCommand(
    val email: String,
    val password: String,
)

data class RefreshCommand(
    val refreshToken: String,
)
