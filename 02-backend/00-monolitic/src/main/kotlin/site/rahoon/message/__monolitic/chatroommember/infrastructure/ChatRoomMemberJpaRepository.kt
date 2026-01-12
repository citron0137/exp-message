package site.rahoon.message.__monolitic.chatroommember.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

/**
 * Spring Data JPA Repository
 */
interface ChatRoomMemberJpaRepository : JpaRepository<ChatRoomMemberEntity, String> {
    fun findByChatRoomIdAndUserId(chatRoomId: String, userId: String): ChatRoomMemberEntity?
    fun findByChatRoomId(chatRoomId: String): List<ChatRoomMemberEntity>
    fun findByUserId(userId: String): List<ChatRoomMemberEntity>
    fun deleteByChatRoomIdAndUserId(chatRoomId: String, userId: String)
}
