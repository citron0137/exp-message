package site.rahoon.message.monolithic.message.longpolling

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.async.DeferredResult
import site.rahoon.message.monolithic.common.controller.CommonAuthInfo
import site.rahoon.message.monolithic.message.controller.MessageResponse

/**
 * Long Polling 기반 실시간 메시지 수신 엔드포인트.
 *
 * - GET /long-polling/chat-rooms/{chatRoomId}/messages
 * - Authorization 헤더로 인증
 * - 새 메시지가 있을 때까지 대기 (최대 30초)
 * - 새 메시지가 생성되면 즉시 응답, 타임아웃 시 빈 리스트 응답
 */
@RestController
@RequestMapping("/long-polling/chat-rooms/{chatRoomId}")
class MessageLongPollingController(
    private val longPollingManager: MessageLongPollingManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 특정 채팅방의 새 메시지 대기 (Long Polling).
     * - 타임아웃: 30초 (30000ms), 테스트용으로 timeout 파라미터 지정 가능
     * - 새 메시지 생성 시 MessageCreatedLongPollingEventListener가 응답
     * - 타임아웃 시 빈 리스트 [] 응답
     *
     * @param chatRoomId 구독할 채팅방 ID (경로 변수)
     * @param timeout 타임아웃(ms), 기본값 30초 (테스트용 옵션)
     * @param authInfo 인증된 사용자 정보 (AuthInfoArgumentResolver가 주입)
     * @return DeferredResult<List<MessageResponse.Detail>> (Spring이 자동으로 비동기 응답 처리)
     */
    @GetMapping("/messages")
    @Suppress("UnusedParameter")
    fun pollMessages(
        @PathVariable chatRoomId: String,
        @RequestParam(required = false) timeout: Long?,
        authInfo: CommonAuthInfo,
    ): DeferredResult<List<MessageResponse.Detail>> {
        val timeoutMs = timeout ?: TIMEOUT_MS
        val deferredResult = DeferredResult<List<MessageResponse.Detail>>(timeoutMs)

        log.debug("Long polling request received. chatRoomId={}, timeout={}ms", chatRoomId, timeoutMs)

        return longPollingManager.add(chatRoomId, deferredResult)
    }

    companion object {
        private const val TIMEOUT_MS = 30000L // 30초
    }
}
