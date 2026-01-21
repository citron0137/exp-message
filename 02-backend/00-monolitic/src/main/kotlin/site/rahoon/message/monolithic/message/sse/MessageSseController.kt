package site.rahoon.message.monolithic.message.sse

import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import site.rahoon.message.monolithic.common.controller.CommonAuthInfo

/**
 * SSE 기반 실시간 메시지 스트림 엔드포인트.
 *
 * - GET /sse/chat-rooms/{chatRoomId}/messages
 * - Authorization 헤더로 인증
 * - 채팅방별 메시지 생성 시 실시간 수신
 */
@RestController
@RequestMapping("/sse/chat-rooms/{chatRoomId}")
class MessageSseController(
    private val sseEmitterManager: MessageSseEmitterManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val TIMEOUT_MS = 300000L // 5분
    }

    /**
     * 특정 채팅방의 메시지 스트림 구독.
     * - 타임아웃: 5분 (300000ms), 테스트용으로 timeout 파라미터 지정 가능
     * - 메시지 생성 시 MessageCreatedSseEventListener가 브로드캐스트
     *
     * @param chatRoomId 구독할 채팅방 ID (경로 변수)
     * @param timeout 타임아웃(ms), 기본값 5분 (테스트용 옵션)
     * @param authInfo 인증된 사용자 정보 (AuthInfoArgumentResolver가 주입)
     * @param response HTTP 응답 객체 (헤더 설정용)
     * @return SseEmitter (Spring이 자동으로 응답 스트림으로 변환)
     */
    @GetMapping("/messages", produces = ["text/event-stream"])
    @Suppress("UnusedParameter")
    fun streamMessages(
        @PathVariable chatRoomId: String,
        @RequestParam(required = false) timeout: Long?,
        authInfo: CommonAuthInfo,
        response: HttpServletResponse,
    ): SseEmitter {
        response.setHeader("Cache-Control", "no-cache, no-transform")
        response.setHeader("X-Accel-Buffering", "no") // Nginx 프록시 버퍼링 비활성화
        response.setHeader("Connection", "keep-alive")
        // response.setHeader("Transfer-Encoding", "chunked")

        val timeoutMs = timeout ?: TIMEOUT_MS
        val emitter = SseEmitter(timeoutMs)
        sseEmitterManager.add(chatRoomId, emitter)
        emitter.send(SseEmitter.event().name("connected").data("true"))
        return emitter
    }

}
