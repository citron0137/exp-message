package site.rahoon.message.monolithic.common.websocket.reply

/**
 * WebSocket Reply 전송 시 사용하는 래퍼 타입.
 *
 * [@WebSocketReply][site.rahoon.message.monolithic.common.websocket.annotation.WebSocketReply] 가 붙은 메서드가
 * 이 타입을 반환하면 payload를 destination으로 전송하고, [receiptId]가 있으면 메시지 헤더에 설정한다.
 *
 * @param T 전송할 payload 타입
 * @param payload 실제 전송할 데이터
 * @param receiptId STOMP receipt-id (선택, 클라이언트 요청의 receipt와 매칭용)
 * @param requestDestination 이 reply를 유발한 요청의 destination (클라이언트가 SEND한 경로, 예: /app/test/echo). Reply 전송 경로가 아님.
 * @param websocketSessionId reply destination 템플릿의 {websocketSessionId} 치환용 (예: "/queue/session/{websocketSessionId}/reply")
 */
data class WebSocketReplyBody<out T>(
    val payload: T,
    val receiptId: String? = null,
    val requestDestination: String? = null,
    val websocketSessionId: String? = null,
)
