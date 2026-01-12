package site.rahoon.message.__monolitic.chatroommember.controller

import java.time.LocalDateTime
import site.rahoon.message.__monolitic.chatroommember.domain.ChatRoomMemberInfo

/**
 * ChatRoomMember Controller 응답 DTO
 */
object ChatRoomMemberResponse {
    /**
     * 채팅방 참가/나가기 응답
     */
    data class Member(
        val id: String,
        val chatRoomId: String,
        val userId: String,
        val joinedAt: LocalDateTime
    ) {
        companion object {
            /**
             * ChatRoomMemberInfo.Detail로부터 ChatRoomMemberResponse.Member를 생성합니다.
             */
            fun from(memberInfo: ChatRoomMemberInfo.Detail): Member {
                return Member(
                    id = memberInfo.id,
                    chatRoomId = memberInfo.chatRoomId,
                    userId = memberInfo.userId,
                    joinedAt = memberInfo.joinedAt
                )
            }
        }
    }

    /**
     * 채팅방 멤버 목록 응답
     */
    data class ListItem(
        val id: String,
        val chatRoomId: String,
        val userId: String,
        val joinedAt: LocalDateTime
    ) {
        companion object {
            /**
             * ChatRoomMemberInfo.Detail로부터 ChatRoomMemberResponse.ListItem을 생성합니다.
             */
            fun from(memberInfo: ChatRoomMemberInfo.Detail): ListItem {
                return ListItem(
                    id = memberInfo.id,
                    chatRoomId = memberInfo.chatRoomId,
                    userId = memberInfo.userId,
                    joinedAt = memberInfo.joinedAt
                )
            }
        }
    }
}
