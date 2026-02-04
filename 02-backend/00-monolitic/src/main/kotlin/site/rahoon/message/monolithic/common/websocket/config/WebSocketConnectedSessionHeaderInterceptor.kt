package site.rahoon.message.monolithic.common.websocket.config

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.MessageBuilder
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.SimpMessageType
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.core.Ordered
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component

/**
 * clientOutboundChannel에서 CONNECT_ACK 메시지를 CONNECTED 헤더(세션 포함)로 만든 메시지로 교체한다.
 *
 * Spring [StompSubProtocolHandler]는 CONNECTED 프레임에 `session` 헤더를 넣지 않으므로,
 * CONNECT_ACK가 핸들러에 도달하기 전에 CONNECTED 헤더(version, heart-beat, session)를 가진 메시지로
 * 바꿔 두면, 핸들러가 그대로 인코딩해 클라이언트에 session이 포함된 CONNECTED를 보낸다.
 */
@Component
class WebSocketConnectedSessionHeaderInterceptor : ChannelInterceptor, Ordered {

    companion object {
        private val SUPPORTED_VERSIONS = arrayOf("1.2", "1.1", "1.0")
    }

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        if (SimpMessageHeaderAccessor.getMessageType(message.headers) != SimpMessageType.CONNECT_ACK) {
            return message
        }
        val payload = message.payload
        if (payload !is ByteArray) {
            return message
        }
        val connectedAccessor = buildConnectedHeadersWithSession(message) ?: return message
        return MessageBuilder.createMessage(payload, connectedAccessor.messageHeaders)
    }

    private fun buildConnectedHeadersWithSession(connectAckMessage: Message<*>): StompHeaderAccessor? {
        val connectMessage = connectAckMessage.headers[StompHeaderAccessor.CONNECT_MESSAGE_HEADER] as? Message<*>
            ?: return null
        val connectHeaders = StompHeaderAccessor.wrap(connectMessage)
        val connectedAccessor = StompHeaderAccessor.create(StompCommand.CONNECTED)

        val acceptVersions = connectHeaders.acceptVersion
        val version = SUPPORTED_VERSIONS.firstOrNull { acceptVersions?.contains(it) == true }
            ?: return null
        connectedAccessor.version = version

        @Suppress("UNCHECKED_CAST")
        val heartbeat = connectAckMessage.headers[SimpMessageHeaderAccessor.HEART_BEAT_HEADER] as? LongArray
        if (heartbeat != null && heartbeat.size >= 2) {
            connectedAccessor.setHeartbeat(heartbeat[0], heartbeat[1])
        } else {
            connectedAccessor.setHeartbeat(0L, 0L)
        }

        val sessionId = SimpMessageHeaderAccessor.getSessionId(connectAckMessage.headers)
        if (sessionId != null) {
            connectedAccessor.sessionId = sessionId  // SubProtocolWebSocketHandler 라우팅용
            connectedAccessor.setNativeHeader("session", sessionId)  // 클라이언트에 내려줄 STOMP 헤더
        }

        return connectedAccessor
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE
}
