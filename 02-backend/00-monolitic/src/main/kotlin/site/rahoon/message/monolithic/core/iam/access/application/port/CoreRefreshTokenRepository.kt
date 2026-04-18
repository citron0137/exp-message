package site.rahoon.message.monolithic.core.iam.access.application.port

import site.rahoon.message.monolithic.core.iam.access.domain.CoreRefreshToken

interface CoreRefreshTokenRepository {
    /**
     * Saves a refresh token.
     */
    fun save(refreshToken: CoreRefreshToken): CoreRefreshToken

    /**
     * Finds a refresh token by token string.
     */
    fun findByToken(token: String): CoreRefreshToken?

    /**
     * Deletes a refresh token by token string.
     */
    fun deleteByToken(token: String)

    /**
     * Deletes all refresh tokens for a session.
     */
    fun deleteBySessionId(sessionId: String)
}
