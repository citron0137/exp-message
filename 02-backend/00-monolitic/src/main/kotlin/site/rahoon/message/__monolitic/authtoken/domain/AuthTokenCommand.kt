package site.rahoon.message.__monolitic.authtoken.domain

sealed class AuthTokenCommand {
    /**
     * 로그인 명령
     * 이메일과 비밀번호로 로그인하여 토큰을 발급받습니다.
     */
    data class Login(
        val email: String,
        val password: String // raw password
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

