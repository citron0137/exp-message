package site.rahoon.message.__monolitic.authtoken.domain

import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthTokenDomainService (
    private val accessTokenIssuer: AccessTokenIssuer,
    private val accessTokenVerifier: AccessTokenVerifier,
    private val refreshTokenIssuer: RefreshTokenIssuer,
    private val authTokenRepository: AuthTokenRepository,
){

    fun issueToken(userId: String): AuthToken{
        val sessionId = UUID.randomUUID().toString()
        val accessToken = accessTokenIssuer.issue(userId, sessionId)
        val refreshToken = refreshTokenIssuer.issue(userId, sessionId)
        authTokenRepository.saveRefreshToken(refreshToken)
        return AuthToken(accessToken, refreshToken)
    }

    fun verifyAccessToken(accessToken: String): AccessToken {
        return accessTokenVerifier.verify(accessToken)
    }
}