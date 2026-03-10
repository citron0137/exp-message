package site.rahoon.message.monolithic.authtoken.controller

import site.rahoon.message.monolithic.authtoken.domain.AuthToken
import java.time.LocalDateTime

/**
 * Admin Web Auth 응답 DTO
 * refresh token은 HttpOnly cookie로만 전달합니다.
 */
object AdminWebAuthResponse {
    data class Login(
        val accessToken: String,
        val accessTokenExpiresAt: LocalDateTime,
        val userId: String,
        val sessionId: String,
        val role: String,
    ) {
        companion object {
            fun from(authToken: AuthToken): Login =
                Login(
                    accessToken = authToken.accessToken.token,
                    accessTokenExpiresAt = authToken.accessToken.expiresAt,
                    userId = authToken.accessToken.userId,
                    sessionId = authToken.accessToken.sessionId,
                    role = authToken.accessToken.role,
                )
        }
    }

    data class Logout(
        val message: String = "로그아웃되었습니다",
    )
}
