package site.rahoon.message.__monolitic.authtoken.controller

import java.time.LocalDateTime
import site.rahoon.message.__monolitic.authtoken.domain.AuthToken

/**
 * Auth Controller 응답 DTO
 */
object AuthResponse {
    /**
     * 로그인 응답
     */
    data class Login(
        val accessToken: String,
        val accessTokenExpiresAt: LocalDateTime,
        val refreshToken: String?,
        val refreshTokenExpiresAt: LocalDateTime?,
        val userId: String,
        val sessionId: String
    ) {
        companion object {
            /**
             * AuthToken으로부터 AuthResponse.Login을 생성합니다.
             */
            fun from(authToken: AuthToken): Login {
                return Login(
                    accessToken = authToken.accessToken.token,
                    accessTokenExpiresAt = authToken.accessToken.expiresAt,
                    refreshToken = authToken.refreshToken?.token,
                    refreshTokenExpiresAt = authToken.refreshToken?.expiresAt,
                    userId = authToken.accessToken.userId,
                    sessionId = authToken.accessToken.sessionId
                )
            }
        }
    }

    /**
     * 로그아웃 응답
     */
    data class Logout(
        val message: String = "로그아웃되었습니다"
    )
}

