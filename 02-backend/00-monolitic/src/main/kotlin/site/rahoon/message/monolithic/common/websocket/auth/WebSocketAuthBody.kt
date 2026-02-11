package site.rahoon.message.monolithic.common.websocket.auth

import java.time.ZonedDateTime

/**
 * WebSocket auth 큐 MESSAGE payload용 DTO.
 *
 * `/queue/session/{websocketSessionId}/auth`로 전송되는 갱신 유도 등 인증 관련 알림.
 * reply·exception과 동일한 정형 구조.
 *
 * @param event 이벤트 타입. 현재 값: [EVENT_TOKEN_EXPIRING_SOON]. 향후 확장용.
 * @param expiresAt 토큰 만료 시각. ISO-8601 형식.
 * @param websocketSessionId 대상 세션 ID. reply·exception과 동일. 클라이언트 식별·매칭용.
 * @param occurredAt 서버 발송 시각. ISO-8601. 디버깅·로깅용.
 */
data class WebSocketAuthBody(
    val event: String,
    val expiresAt: String,
    val websocketSessionId: String,
    val occurredAt: ZonedDateTime? = null,
) {
    companion object {
        /** 토큰 만료 임박 시 갱신 유도 이벤트. */
        const val EVENT_TOKEN_EXPIRING_SOON = "token_expiring_soon"
    }
}
