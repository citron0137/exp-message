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
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.websocket.config.WebSocketAnnotatedMethodInvoker

/**
 * SUBSCRIBE 시 destination에 대한 권한 검증.
 *
 * - `/topic/user/{uuid}/...`: uuid가 [CommonAuthInfo.userId]와 일치할 때만 구독 허용. 허용 시 [WebsocketSubscribe] 호출.
 * - `/queue/session/{sessionId}/reply`, `/queue/session/{sessionId}/exception`: sessionId가 현재 세션 ID와 일치할 때만 구독 허용.
 *   불일치 시 DomainException이 아닌 일반 예외를 던져 ERROR 프레임으로 연결만 끊는다 (상세 payload·exception 큐 전송 없음).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
class WebSocketTopicSubscribeInterceptor(
    private val annotatedMethodInvoker: WebSocketAnnotatedMethodInvoker,
) : ChannelInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val USER_TOPIC_PATTERN = Regex("^/topic/user/([^/]+)(/.*)?$")
        private val SESSION_QUEUE_PATTERN = Regex("^/queue/session/([^/]+)/(reply|exception)$")
    }

    @Nullable
    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command != StompCommand.SUBSCRIBE) return message

        val destination = accessor.destination ?: return message

        val sessionQueueMatch = SESSION_QUEUE_PATTERN.find(destination)
        if (sessionQueueMatch != null) {
            val pathSessionId = sessionQueueMatch.groupValues[1]
            val currentSessionId = accessor.sessionId
            if (currentSessionId == null || pathSessionId != currentSessionId) {
                log.warn("구독 거부: session queue의 sessionId 불일치, destination=$destination, sessionId=$currentSessionId")
                throw IllegalStateException("Subscription denied: session queue sessionId mismatch")
            }
            return message
        }

        val match = USER_TOPIC_PATTERN.find(destination) ?: return message

        val topicUserId = match.groupValues[1]
        val authInfo = accessor.sessionAttributes?.get(WebSocketAuthHandshakeHandler.ATTR_AUTH_INFO) as? CommonAuthInfo
        val principalUserId = authInfo?.userId

        if (principalUserId == null || principalUserId != topicUserId) {
            log.warn("구독 거부: destination=$destination, principalUserId=$principalUserId, topicUserId=$topicUserId")
            throw DomainException(
                CommonError.CLIENT_ERROR,
                mapOf(
                    "reason" to "해당 topic을 구독할 권한이 없습니다. destination의 userId가 로그인 사용자와 일치해야 합니다.",
                    "expectedUserId" to (principalUserId ?: "null"),
                ),
            )
        }
        annotatedMethodInvoker.invokeSubscribe(destination, authInfo!!)
        return message
    }
}
