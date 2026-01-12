package site.rahoon.message.__monolitic.user.controller

import java.time.LocalDateTime
import site.rahoon.message.__monolitic.user.domain.UserInfo

/**
 * User Controller 응답 DTO
 */
object UserResponse {
    /**
     * 회원가입 응답
     */
    data class SignUp(
        val id: String,
        val email: String,
        val nickname: String,
        val createdAt: LocalDateTime
    ) {
        companion object {
            /**
             * UserInfo.Detail로부터 UserResponse.SignUp을 생성합니다.
             */
            fun from(userInfo: UserInfo.Detail): SignUp {
                return SignUp(
                    id = userInfo.id,
                    email = userInfo.email,
                    nickname = userInfo.nickname,
                    createdAt = userInfo.createdAt
                )
            }
        }
    }

    /**
     * 현재 로그인한 사용자 정보 응답
     */
    data class Me(
        val id: String,
        val email: String,
        val nickname: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime
    ) {
        companion object {
            /**
             * UserInfo.Detail로부터 UserResponse.Me를 생성합니다.
             */
            fun from(userInfo: UserInfo.Detail): Me {
                return Me(
                    id = userInfo.id,
                    email = userInfo.email,
                    nickname = userInfo.nickname,
                    createdAt = userInfo.createdAt,
                    updatedAt = userInfo.updatedAt
                )
            }
        }
    }
}

