package site.rahoon.message.monolithic.message.sse

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * SSE Emitter를 채팅방별로 관리하는 매니저.
 * - 구독(add): 특정 채팅방에 대한 SseEmitter 추가
 * - 전송(send): 특정 채팅방의 모든 구독자에게 데이터 전송
 * - 해제(remove): 연결 종료 시 SseEmitter 제거
 */
@Component
class MessageSseEmitterManager(
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // chatRoomId -> Set<SseEmitter>
    private val emitters = ConcurrentHashMap<String, MutableSet<SseEmitter>>()

    /**
     * 특정 채팅방에 SseEmitter 추가.
     * @return 추가된 SseEmitter (타임아웃/완료/에러 시 자동 제거 콜백 등록됨)
     */
    fun add(
        chatRoomId: String,
        emitter: SseEmitter,
    ): SseEmitter {
        emitters.computeIfAbsent(chatRoomId) { ConcurrentHashMap.newKeySet() }.add(emitter)
        log.debug("SSE emitter added. chatRoomId={}, total={}", chatRoomId, emitters[chatRoomId]?.size)

        // 자동 제거 콜백
        emitter.onCompletion { remove(chatRoomId, emitter) }
        emitter.onTimeout { remove(chatRoomId, emitter) }
        emitter.onError { remove(chatRoomId, emitter) }

        return emitter
    }

    /**
     * 특정 채팅방의 모든 구독자에게 데이터 전송.
     * - 전송 실패한 emitter는 자동 제거
     * - 데이터를 JSON 문자열로 직렬화해서 전송
     */
    fun send(
        chatRoomId: String,
        data: Any,
    ) {
        val targets = emitters[chatRoomId] ?: return
        log.debug("Sending SSE event to chatRoomId={}, subscribers={}", chatRoomId, targets.size)

        // JSON 문자열로 직렬화
        val jsonData = objectMapper.writeValueAsString(data)
        val iterator = targets.iterator()
        while (iterator.hasNext()) {
            val emitter = iterator.next()
            try {
                emitter.send(
                    SseEmitter
                        .event()
                        .name("message")
                        .data(jsonData, MediaType.APPLICATION_JSON),
                )
                log.debug("SSE event sent to chatRoomId={}, subscriber={}", chatRoomId, emitter)
            } catch (e: Exception) {
                // IOException 뿐만 아니라 모든 예외 발생 시 해당 Emitter 제거
                log.debug("SSE 전송 실패로 인한 Emitter 제거. RoomId: {}, Error: {}", chatRoomId, e.message)
                remove(chatRoomId, emitter)
            }
        }
    }

    /**
     * 특정 채팅방에서 SseEmitter 제거.
     */
    private fun remove(
        chatRoomId: String,
        emitter: SseEmitter,
    ) {
        emitters[chatRoomId]?.remove(emitter)
        if (emitters[chatRoomId]?.isEmpty() == true) {
            emitters.remove(chatRoomId)
        }
        log.debug("SSE emitter removed. chatRoomId={}, remaining={}", chatRoomId, emitters[chatRoomId]?.size ?: 0)
        runCatching {
            emitter.complete()
        }.onFailure { secondaryError ->
            log.debug("SSE emitter complete after remove error. chatRoomId={}, error={}", chatRoomId, secondaryError.message)
        }
    }
}
