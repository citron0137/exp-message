package site.rahoon.message.__monolitic.authtoken.domain

interface AuthTokenRepository {
    fun saveRefreshToken(refreshToken: RefreshToken)
    fun deleteRefreshTokenBySessionId(sessionId: String)
}


