package site.rahoon.message.__monolitic.authtoken.domain

sealed class AuthTokenCommand {
    /**
     * 토큰 발급 명령
     * (인증/사용자 검증은 application 레이어에서 수행하고)
     * 검증된 사용자 식별자만을 받아 토큰을 발급합니다.
     */
    data class Issue(
        val userId: String
    ) : AuthTokenCommand()

    /**
     * 토큰 갱신 명령
     * 리프레시 토큰으로 새로운 액세스 토큰을 발급받습니다.
     */
    data class Refresh(
        val refreshToken: String
    ) : AuthTokenCommand()

    /**
     * 로그아웃 명령
     * 토큰을 무효화합니다.
     */
    data class Logout(
        val accessToken: String
    ) : AuthTokenCommand()
}

