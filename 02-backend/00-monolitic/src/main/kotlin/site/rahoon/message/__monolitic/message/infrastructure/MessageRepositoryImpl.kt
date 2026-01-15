package site.rahoon.message.__monolitic.message.infrastructure

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import site.rahoon.message.__monolitic.message.domain.Message
import site.rahoon.message.__monolitic.message.domain.MessageRepository
import java.time.LocalDateTime

/**
 * MessageRepository 인터페이스의 JPA 구현체
 */
@Repository
class MessageRepositoryImpl(
    private val jpaRepository: MessageJpaRepository
) : MessageRepository {

    override fun save(message: Message): Message {
        val entity = toEntity(message)
        val savedEntity = jpaRepository.save(entity)
        return toDomain(savedEntity)
    }

    override fun findById(id: String): Message? {
        return jpaRepository.findById(id)
            .map { toDomain(it) }
            .orElse(null)
    }

    override fun findPageByChatRoomId(
        chatRoomId: String,
        afterCreatedAt: LocalDateTime?,
        afterId: String?,
        limit: Int
    ): List<Message> {
        val pageable = PageRequest.of(
            0,
            limit,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        )

        val entities = if (afterCreatedAt == null || afterId == null) {
            jpaRepository.findByChatRoomIdOrderByCreatedAtDescIdDesc(chatRoomId, pageable)
        } else {
            jpaRepository.findNextPageByChatRoomId(chatRoomId, afterCreatedAt, afterId, pageable)
        }

        return entities.map { toDomain(it) }
    }

    private fun toEntity(message: Message): MessageEntity {
        return MessageEntity(
            id = message.id,
            chatRoomId = message.chatRoomId,
            userId = message.userId,
            content = message.content,
            createdAt = message.createdAt
        )
    }

    private fun toDomain(entity: MessageEntity): Message {
        return Message(
            id = entity.id,
            chatRoomId = entity.chatRoomId,
            userId = entity.userId,
            content = entity.content,
            createdAt = entity.createdAt
        )
    }
}
