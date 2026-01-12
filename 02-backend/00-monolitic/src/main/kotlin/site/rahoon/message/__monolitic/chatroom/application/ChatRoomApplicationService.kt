package site.rahoon.message.__monolitic.chatroom.application

import org.springframework.stereotype.Service
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomCommand
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomDomainService
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomError
import site.rahoon.message.__monolitic.chatroom.domain.ChatRoomInfo
import site.rahoon.message.__monolitic.common.domain.DomainException

/**
 * ChatRoom Application Service
 * 어플리케이션 레이어에서 도메인 서비스를 호출하고 결과를 반환합니다.
 */
@Service
class ChatRoomApplicationService(
    private val chatRoomDomainService: ChatRoomDomainService
) {

    /**
     * 채팅방 생성
     */
    fun create(criteria: ChatRoomCriteria.Create): ChatRoomInfo.Detail {
        val command = criteria.toCommand()
        // TODO ChatRoomUser에 본인 추가 필요.
        return chatRoomDomainService.create(command)
    }

    /**
     * 채팅방 수정
     */
    fun update(criteria: ChatRoomCriteria.Update): ChatRoomInfo.Detail {
        // 권한 검증: 생성자만 수정 가능
        val chatRoomInfo = chatRoomDomainService.getById(criteria.chatRoomId)
        if (chatRoomInfo.createdByUserId != criteria.userId) {
            throw DomainException(
                error = ChatRoomError.UNAUTHORIZED_ACCESS,
                details = mapOf(
                    "chatRoomId" to criteria.chatRoomId,
                    "userId" to criteria.userId,
                    "reason" to "채팅방 생성자만 수정할 수 있습니다"
                )
            )
        }

        val command = criteria.toCommand()
        return chatRoomDomainService.update(command)
    }

    /**
     * 채팅방 삭제
     */
    fun delete(criteria: ChatRoomCriteria.Delete): ChatRoomInfo.Detail {
        // 권한 검증: 생성자만 삭제 가능
        val chatRoomInfo = chatRoomDomainService.getById(criteria.chatRoomId)
        if (chatRoomInfo.createdByUserId != criteria.userId) {
            throw DomainException(
                error = ChatRoomError.UNAUTHORIZED_ACCESS,
                details = mapOf(
                    "chatRoomId" to criteria.chatRoomId,
                    "userId" to criteria.userId,
                    "reason" to "채팅방 생성자만 삭제할 수 있습니다"
                )
            )
        }

        val command = criteria.toCommand()
        return chatRoomDomainService.delete(command)
    }

    /**
     * 채팅방 조회
     */
    fun getById(chatRoomId: String): ChatRoomInfo.Detail {
        return chatRoomDomainService.getById(chatRoomId)
    }

    /**
     * 내가 생성한 채팅방 목록 조회
     */
    fun getByCreatedByUserId(userId: String): List<ChatRoomInfo.Detail> {
        return chatRoomDomainService.getByCreatedByUserId(userId)
    }
}
