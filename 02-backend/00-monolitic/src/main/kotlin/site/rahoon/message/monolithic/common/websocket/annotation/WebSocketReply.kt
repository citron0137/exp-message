package site.rahoon.message.monolithic.common.websocket.annotation

/**
 * Reply 전용 WebSocket 전송 어노테이션.
 *
 * 내부 동작은 [WebsocketSend]와 동일하게 지정된 destination으로 전송한다.
 *
 * @param value WebSocket destination 경로 (기본값: "/queue/session/{websocketSessionId}/reply")
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebSocketReply(
    val value: String = "/queue/session/{websocketSessionId}/reply",
)

