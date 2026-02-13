package site.rahoon.message.monolithic.common.websocket.exception

import java.time.ZonedDateTime

/**
 * WebSocket ERROR 프레임 payload용 DTO.
 *
 * STOMP ERROR 프레임 body에 담길 `{ "code", "message", "details"? }` 형태.
 * [occurredAt], [websocketSessionId], [receiptId], [requestDestination]이 있으면 payload에 포함된다.
 */
data class WebSocketExceptionBody(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null,
    /** 예외 발생 시각 (ISO-8601). */
    val occurredAt: ZonedDateTime? = null,
    val websocketSessionId: String? = null,
    val receiptId: String? = null,
    /** 이 예외를 유발한 요청의 destination (클라이언트가 SEND한 경로, 예: /app/test/echo). */
    val requestDestination: String? = null,
)
