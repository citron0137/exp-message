package site.rahoon.message.__monolitic.chatroommember.application

import site.rahoon.message.__monolitic.chatroommember.domain.ChatRoomMemberCommand

/**
 * ChatRoomMember Application Layer 입력 DTO
 */
object ChatRoomMemberCriteria {
    data class Join(
        val chatRoomId: String,
        val userId: String
    ) {
        /**
         * ChatRoomMemberCommand.Join으로 변환합니다.
         */
        fun toCommand(): ChatRoomMemberCommand.Join {
            return ChatRoomMemberCommand.Join(
                chatRoomId = this.chatRoomId,
                userId = this.userId
            )
        }
    }

    data class Leave(
        val chatRoomId: String,
        val userId: String
    ) {
        /**
         * ChatRoomMemberCommand.Leave로 변환합니다.
         */
        fun toCommand(): ChatRoomMemberCommand.Leave {
            return ChatRoomMemberCommand.Leave(
                chatRoomId = this.chatRoomId,
                userId = this.userId
            )
        }
    }
}
