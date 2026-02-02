package site.rahoon.message.monolithic.common.websocket.config

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.lang.Nullable
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.websocket.config.WebSocketAnnotatedMethodInvoker

/**
 * SUBSCRIBE 시 `/topic/user/{uuid}/...` destination에 대한 권한 검증.
 * destination의 uuid가 세션 속성 [WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO](CommonAuthInfo.userId)와 일치할 때만 구독 허용.
 * 허용 시 [WebSocketAnnotatedMethodInvoker]로 [WebsocketSubscribe] 메서드들을 호출.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class WebSocketTopicSubscribeInterceptor(
    private val annotatedMethodInvoker: WebSocketAnnotatedMethodInvoker,
) : ChannelInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val USER_TOPIC_PATTERN = Regex("^/topic/user/([^/]+)(/.*)?$")
    }

    @Nullable
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command != StompCommand.SUBSCRIBE) return message

        val destination = accessor.destination ?: return message
        val match = USER_TOPIC_PATTERN.find(destination) ?: return message

        val topicUserId = match.groupValues[1]
        val authInfo = accessor.sessionAttributes?.get(WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO) as? CommonAuthInfo
        val principalUserId = authInfo?.userId

        if (principalUserId == null || principalUserId != topicUserId) {
            log.warn("구독 거부: destination=$destination, principalUserId=$principalUserId, topicUserId=$topicUserId")
            return null
        }
        annotatedMethodInvoker.invokeSubscribe(destination, authInfo!!)
        return message
    }
}
