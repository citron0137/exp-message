package site.rahoon.message.__monolitic.authtoken.application

/**
 * AuthToken Application Criteria
 * 외부(Controller 등)에서 들어오는 요청을 application 레이어에서 받기 위한 DTO 성격의 객체입니다.
 *
 * - 인증(이메일/비밀번호 검증)과 도메인 조합은 application 레이어에서 처리합니다.
 * - AuthToken 도메인 커맨드는 "검증된 주체"만 받도록 제한합니다.
 */
sealed class AuthTokenCriteria {
    data class Login(
        val email: String,
        val password: String
    ) : AuthTokenCriteria()

    data class Refresh(
        val refreshToken: String
    ) : AuthTokenCriteria()

    data class Logout(
        /**
         * Authorization 헤더 값 또는 순수 토큰 값 둘 다 허용 (예: "Bearer xxx" / "xxx")
         */
        val accessToken: String
    ) : AuthTokenCriteria()
}

