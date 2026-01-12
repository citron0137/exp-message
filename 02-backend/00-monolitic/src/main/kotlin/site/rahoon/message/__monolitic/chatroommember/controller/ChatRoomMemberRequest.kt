package site.rahoon.message.__monolitic.chatroommember.controller

import site.rahoon.message.__monolitic.chatroommember.application.ChatRoomMemberCriteria

/**
 * ChatRoomMember Controller 요청 DTO
 */
object ChatRoomMemberRequest {
    /**
     * ChatRoomMemberCriteria.Join으로 변환합니다.
     * (요청 본문이 없으므로 PathVariable과 AuthInfo만 사용)
     */
    fun toJoinCriteria(chatRoomId: String, userId: String): ChatRoomMemberCriteria.Join {
        return ChatRoomMemberCriteria.Join(
            chatRoomId = chatRoomId,
            userId = userId
        )
    }

    /**
     * ChatRoomMemberCriteria.Leave로 변환합니다.
     * (요청 본문이 없으므로 PathVariable과 AuthInfo만 사용)
     */
    fun toLeaveCriteria(chatRoomId: String, userId: String): ChatRoomMemberCriteria.Leave {
        return ChatRoomMemberCriteria.Leave(
            chatRoomId = chatRoomId,
            userId = userId
        )
    }
}
