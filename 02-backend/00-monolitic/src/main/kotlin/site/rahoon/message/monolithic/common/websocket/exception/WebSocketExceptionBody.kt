package site.rahoon.message.monolithic.common.websocket.exception

/**
 * WebSocket ERROR 프레임 payload용 DTO.
 *
 * STOMP ERROR 프레임 body에 담길 `{ "code", "message", "details"? }` 형태.
 * [websocketSessionId], [receiptId]가 있으면 payload에 포함된다.
 */
data class WebSocketExceptionBody(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null,
    val websocketSessionId: String? = null,
    val receiptId: String? = null,
)
