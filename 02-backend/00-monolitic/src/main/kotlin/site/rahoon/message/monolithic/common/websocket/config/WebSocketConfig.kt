package site.rahoon.message.monolithic.common.websocket.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * WebSocket(STOMP) 설정
 *
 * - 엔드포인트: /ws (SockJS fallback)
 * - Broker: /topic (구독 prefix)
 * - Handshake: JWT 검증 후 Principal 설정
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketAuthHandshakeHandler: WebSocketAuthHandshakeHandler,
) : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {

        registry
            .addEndpoint("/ws")
            .setHandshakeHandler(webSocketAuthHandshakeHandler)

        registry
            .addEndpoint("/ws")
            .setHandshakeHandler(webSocketAuthHandshakeHandler)
            .withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {

        val taskScheduler = ThreadPoolTaskScheduler()
        taskScheduler.poolSize = 1
        taskScheduler.setThreadNamePrefix("ws-hb-")
        taskScheduler.initialize()

        registry
            .enableSimpleBroker("/topic")
            .setHeartbeatValue(longArrayOf(10000, 10000))
            .setTaskScheduler(taskScheduler)
    }
}
