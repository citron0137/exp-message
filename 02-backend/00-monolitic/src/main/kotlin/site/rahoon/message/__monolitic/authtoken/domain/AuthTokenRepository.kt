package site.rahoon.message.__monolitic.authtoken.domain

interface AuthTokenRepository {

    // RefreshToken 저장
    fun saveRefreshToken(refreshToken: RefreshToken)
}


