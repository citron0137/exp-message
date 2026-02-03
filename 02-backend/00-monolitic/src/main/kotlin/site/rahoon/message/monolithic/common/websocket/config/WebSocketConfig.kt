package site.rahoon.message.monolithic.common.websocket.config

import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

/**
 * WebSocket(STOMP) 설정
 *
 * - 엔드포인트: /ws (SockJS fallback)
 * - Application destination: /app (SEND 수신, 예: /app/auth/refresh)
 * - Broker: /topic (구독 prefix)
 * - Handshake: 토큰만 세션에 저장. CONNECT 시 [WebSocketConnectInterceptor]에서 토큰 검증·Principal 설정
 * - 구독: WebSocketTopicSubscribeInterceptor로 /topic/user/{uuid}/... 본인 토픽만 허용
 */
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val webSocketAuthHandshakeHandler: WebSocketAuthHandshakeHandler,
    private val webSocketConnectInterceptor: WebSocketConnectInterceptor,
    private val webSocketTopicSubscribeInterceptor: WebSocketTopicSubscribeInterceptor,
    private val webSocketStompErrorHandler: WebSocketStompErrorHandler,
    private val webSocketClientInboundErrorInterceptor: WebSocketClientInboundErrorInterceptor,
) : WebSocketMessageBrokerConfigurer {
    companion object {
        private const val HEARTBEAT_INTERVAL_MS = 10000L
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint("/ws")
            .setHandshakeHandler(webSocketAuthHandshakeHandler)

        registry
            .addEndpoint("/ws")
            .setHandshakeHandler(webSocketAuthHandshakeHandler)
            .withSockJS()

        // 엔드포인트 등록 후 에러 핸들러 설정 (SEND 처리 중 @MessageMapping 예외 → ERROR 프레임)
        registry.setErrorHandler(webSocketStompErrorHandler)
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration
            .interceptors(
                webSocketConnectInterceptor,
                webSocketTopicSubscribeInterceptor,
                webSocketClientInboundErrorInterceptor,
            )
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.setApplicationDestinationPrefixes("/app")

        val taskScheduler = ThreadPoolTaskScheduler()
        taskScheduler.poolSize = 1
        taskScheduler.setThreadNamePrefix("ws-hb-")
        taskScheduler.initialize()

        registry
            .enableSimpleBroker("/topic")
            .setHeartbeatValue(longArrayOf(HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS))
            .setTaskScheduler(taskScheduler)
    }
}
