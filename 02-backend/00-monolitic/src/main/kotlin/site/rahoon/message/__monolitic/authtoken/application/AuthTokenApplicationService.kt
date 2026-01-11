package site.rahoon.message.__monolitic.authtoken.application

import org.springframework.stereotype.Service
import site.rahoon.message.__monolitic.authtoken.domain.AccessToken
import site.rahoon.message.__monolitic.authtoken.domain.AuthToken
import site.rahoon.message.__monolitic.user.domain.UserDomainService

/**
 * AuthToken Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class AuthTokenApplicationService(
    private val userDomainService: UserDomainService,
){

    // Check
    fun checkAccessToken( accessToken: String ): AccessToken {
        // return authTokenDomainService.verifyAccessToken(accessToken)
        TODO()
    }

    // Login
    fun login( email: String, password: String ): AuthToken {
        // 이메일/비밀번호 검증 -> 토큰 발급 -> 반환
        val user = userDomainService.getUser( email, password )
        // return authTokenDomainService.issueToken(user.id)
        TODO()
    }

    // Refresh
    fun refresh( refreshToken: String ): AuthToken {
        // val refreshTokenClaim = authTokenDomainService.verifyRefreshToken(refreshToken)
        // val newToken = authTokenDomainService.issueToken(
        //      refreshTokenClaim.userId,
        //      refreshTokenClaim.sessionId
        // )
        // authTokenDomainService.expireRefreshToken(refreshToken)
        TODO()
    }

    // Logout
    fun logout( sessionId: String ){
        // authTokenDomainService.expireBySessionId(sessionId)
        TODO()
    }
}

