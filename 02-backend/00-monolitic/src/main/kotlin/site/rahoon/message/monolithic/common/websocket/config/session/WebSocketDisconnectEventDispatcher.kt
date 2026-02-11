package site.rahoon.message.monolithic.common.websocket.config.session

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.websocket.config.auth.WebSocketAuthHandshakeHandler
import site.rahoon.message.monolithic.common.websocket.config.subscribe.WebSocketAnnotatedMethodInvoker

/**
 * [SessionDisconnectEvent] 수신 시 세션의 [WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO](CommonAuthInfo)를 꺼내
 * [WebSocketAnnotatedMethodInvoker]로 [WebsocketDisconnected] 메서드들을 호출한다.
 */
@Component
class WebSocketDisconnectEventDispatcher(
    private val annotatedMethodInvoker: WebSocketAnnotatedMethodInvoker,
    private val sessionAuthInfoRegistry: WebSocketSessionAuthInfoRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun onDisconnect(event: SessionDisconnectEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        accessor.sessionId?.let { sessionAuthInfoRegistry.unregister(it) }
        val authInfo = accessor.sessionAttributes?.get(WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO) as? CommonAuthInfo

        if (authInfo != null) {
            log.debug("WebSocket 연결 해제 - userId={}, sessionId={}", authInfo.userId, accessor.sessionId)
            annotatedMethodInvoker.invokeDisconnect(authInfo)
        } else {
            log.warn("WebSocket 연결 해제 시 authInfo 없음 - sessionId={}", accessor.sessionId)
        }
    }
}
