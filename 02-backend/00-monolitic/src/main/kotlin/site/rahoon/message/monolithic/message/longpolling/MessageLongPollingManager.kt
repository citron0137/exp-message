package site.rahoon.message.monolithic.message.longpolling

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.context.request.async.DeferredResult
import site.rahoon.message.monolithic.message.controller.MessageResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * Long Polling 요청을 채팅방별로 관리하는 매니저.
 * - 구독(add): 특정 채팅방에 대한 DeferredResult 추가 (새 메시지 대기)
 * - 전송(send): 특정 채팅방의 모든 대기 중인 요청에 데이터 응답
 * - 해제(remove): 타임아웃 시 DeferredResult 제거
 */
@Component
class MessageLongPollingManager(
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // chatRoomId -> Set<DeferredResult<List<MessageResponse.Detail>>>
    private val pendingRequests = ConcurrentHashMap<String, MutableSet<DeferredResult<List<MessageResponse.Detail>>>>()

    /**
     * 특정 채팅방에 Long Polling 요청 추가.
     * @param chatRoomId 채팅방 ID
     * @param deferredResult 대기 중인 비동기 응답
     * @return 추가된 DeferredResult (타임아웃 시 자동 제거 콜백 등록됨)
     */
    fun add(
        chatRoomId: String,
        deferredResult: DeferredResult<List<MessageResponse.Detail>>,
    ): DeferredResult<List<MessageResponse.Detail>> {
        pendingRequests.computeIfAbsent(chatRoomId) { ConcurrentHashMap.newKeySet() }.add(deferredResult)
        log.debug("Long polling request added. chatRoomId={}, total={}", chatRoomId, pendingRequests[chatRoomId]?.size)

        // 자동 제거 콜백
        deferredResult.onCompletion { remove(chatRoomId, deferredResult) }
        deferredResult.onTimeout {
            log.debug("Long polling request timeout. chatRoomId={}", chatRoomId)
            deferredResult.setResult(emptyList()) // 타임아웃 시 빈 리스트 응답
            remove(chatRoomId, deferredResult)
        }
        deferredResult.onError { error ->
            log.warn("Long polling request error. chatRoomId={}, error={}", chatRoomId, error.message)
            remove(chatRoomId, deferredResult)
        }

        return deferredResult
    }

    /**
     * 특정 채팅방의 모든 대기 중인 요청에 새 메시지 전송.
     * - 메시지를 리스트로 감싸서 전송
     * - 전송 후 요청 제거 (Long Polling은 1회성)
     */
    fun send(
        chatRoomId: String,
        message: MessageResponse.Detail,
    ) {
        val targets = pendingRequests[chatRoomId] ?: return
        if (targets.isEmpty()) {
            log.debug("No pending long polling requests for chatRoomId={}", chatRoomId)
            return
        }

        log.debug("Sending message to {} pending long polling requests. chatRoomId={}", targets.size, chatRoomId)

        // 모든 대기 중인 요청에 메시지 전송
        targets.forEach { deferredResult ->
            try {
                deferredResult.setResult(listOf(message))
                log.debug("Message sent to long polling request. chatRoomId={}", chatRoomId)
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception,
            ) {
                log.warn("Failed to send message to long polling request. chatRoomId={}, error={}", chatRoomId, e.message)
            }
        }

        // 전송 후 모두 제거 (Long Polling은 1회성)
        targets.clear()
    }

    /**
     * 특정 채팅방에서 DeferredResult 제거.
     */
    private fun remove(
        chatRoomId: String,
        deferredResult: DeferredResult<List<MessageResponse.Detail>>,
    ) {
        pendingRequests[chatRoomId]?.remove(deferredResult)
        if (pendingRequests[chatRoomId]?.isEmpty() == true) {
            pendingRequests.remove(chatRoomId)
        }
        log.debug("Long polling request removed. chatRoomId={}, remaining={}", chatRoomId, pendingRequests[chatRoomId]?.size ?: 0)
    }
}
