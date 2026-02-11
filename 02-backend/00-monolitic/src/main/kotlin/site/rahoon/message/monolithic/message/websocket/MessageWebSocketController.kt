package site.rahoon.message.monolithic.message.websocket

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.websocket.annotation.WebsocketDisconnected
import site.rahoon.message.monolithic.common.websocket.annotation.WebsocketSend
import site.rahoon.message.monolithic.common.websocket.annotation.WebsocketSubscribe
import site.rahoon.message.monolithic.message.application.MessageCommandEvent
import site.rahoon.message.monolithic.message.application.MessageCommandEventRelayPort

/**
 * 메시지 WebSocket 구독·전송 담당.
 *
 * - 구독: 클라이언트가 `/topic/user/{userId}/messages` 구독 시 [MessageCommandEventRelayPort.subscribe] 호출하여 Redis 토픽 리스너 등록.
 * - 전송: Redis에서 [MessageCommandEvent.Send] 수신 시(로컬 이벤트) [@WebsocketSend]로 해당 유저 토픽에 [MessageWsSend.Detail] 브로드캐스트.
 */
@Component
class MessageWebSocketController(
    private val messageCommandEventRelayPort: MessageCommandEventRelayPort,
) {
    /**
     * 클라이언트가 메시지 토픽을 구독할 때 Redis 쪽 구독을 등록한다.
     * [WebSocketTopicSubscribeInterceptor]에서 권한 검증 후 호출됨.
     */
    @WebsocketSubscribe("/topic/user/{userId}/messages")
    fun onMessageTopicSubscribe(
        authInfo: CommonAuthInfo,
        pathVars: Map<String, String>,
    ) {
        val userId = pathVars["userId"] ?: return
        messageCommandEventRelayPort.subscribe(userId)
    }

    /**
     * WebSocket 세션 종료 시 해당 유저의 Redis 메시지 토픽 구독을 해제한다.
     */
    @WebsocketDisconnected
    fun onDisconnect(authInfo: CommonAuthInfo) {
        messageCommandEventRelayPort.unsubscribe(authInfo.userId)
    }

    /**
     * Redis 리스너에서 발행한 [MessageCommandEvent.Send]를 받아 WebSocket으로 전송한다.
     * [MessageCommandEventRelayRedisRepository]의 addListener 콜백에서 publishEvent로 전달된 이벤트 수신.
     */
    @EventListener(MessageCommandEvent.Send::class)
    @WebsocketSend("/topic/user/{recipientUserId}/messages")
    fun sendCreatedMessage(send: MessageCommandEvent.Send): MessageWsSend.Detail =
        MessageWsSend.Detail.from(send)
}
