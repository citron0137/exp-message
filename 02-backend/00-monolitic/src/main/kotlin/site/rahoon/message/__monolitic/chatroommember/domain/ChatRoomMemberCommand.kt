package site.rahoon.message.__monolitic.chatroommember.domain

sealed class ChatRoomMemberCommand {
    data class Join(
        val chatRoomId: String,
        val userId: String
    ) : ChatRoomMemberCommand()

    data class Leave(
        val chatRoomId: String,
        val userId: String
    ) : ChatRoomMemberCommand()
}
