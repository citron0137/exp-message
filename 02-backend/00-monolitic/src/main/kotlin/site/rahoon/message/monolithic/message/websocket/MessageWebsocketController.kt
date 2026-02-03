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
 * WebSocket 메시지 전달 핸들러
 *
 * - [WebsocketSubscribe]: 구독 허용 시 relay 등록
 * - [WebsocketDisconnected]: 세션 끊김 시 relay 해제
 * - [WebsocketSend]: 메시지 생성 이벤트를 `/topic/user/{recipientUserId}/messages`로 전달
 */
@Component
class MessageWebSocketController(
    private val messageCommandEventRelayPort: MessageCommandEventRelayPort,
) {

    @WebsocketSubscribe("/topic/user/{userId}/messages")
    fun onMessageTopicSubscribe(authInfo: CommonAuthInfo, pathVariables: Map<String, String>) {
        messageCommandEventRelayPort.subscribe(authInfo.userId)
    }

    @WebsocketDisconnected
    fun onDisconnect(authInfo: CommonAuthInfo) {
        messageCommandEventRelayPort.unsubscribe(authInfo.userId)
    }

    @EventListener
    @WebsocketSend("/topic/user/{recipientUserId}/messages")
    fun sendCreatedMessage(event: MessageCommandEvent.Send): MessageWsSend.Detail {
        return MessageWsSend.Detail.from(event)
    }
}
