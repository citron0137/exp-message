package site.rahoon.message.__monolitic.authtoken.domain

interface AuthTokenRepository {
    fun saveRefreshToken(refreshToken: RefreshToken): RefreshToken
    fun deleteRefreshTokenBySessionId(sessionId: String)
    fun deleteRefreshToken(refreshToken: String)
    fun findRefreshToken(refreshToken: String):RefreshToken?
}


