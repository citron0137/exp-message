package site.rahoon.message.monolithic.common.websocket.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.lang.Nullable
import org.springframework.messaging.Message
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler
import site.rahoon.message.monolithic.common.domain.DomainException

/**
 * STOMP 메시지 처리 중 발생한 예외를 클라이언트에 전달할 ERROR 메시지로 변환한다.
 *
 * - [DomainException](또는 cause 체인 내 DomainException): ERROR 프레임 payload를
 *   `{ "code": error.code, "message": error.message }` 형태의 JSON으로 내려 보낸다.
 * - 그 외: 기본 [StompSubProtocolErrorHandler] 동작(스택 트레이스 등)을 따른다.
 */
@Component
class WebSocketStompErrorHandler : StompSubProtocolErrorHandler() {
    private val objectMapper = jacksonObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    @Nullable
    override fun handleClientMessageProcessingError(
        clientMessage: Message<ByteArray>?,
        ex: Throwable,
    ): Message<ByteArray>? {
        val domainException = resolveDomainException(ex) ?: return super.handleClientMessageProcessingError(clientMessage, ex)
        log.info("STOMP 메시지 처리 오류 → ERROR 프레임 반환: code={}, message={}", domainException.error.code, domainException.error.message)

        val payload =
            objectMapper.writeValueAsBytes(
                mapOf(
                    "code" to domainException.error.code,
                    "message" to domainException.error.message,
                ),
            )
        val accessor = StompHeaderAccessor.create(StompCommand.ERROR)
        accessor.message = domainException.message

        // clientOutboundChannel 라우팅: resolveSessionId()가 SimpMessageHeaderAccessor.getSessionId() 사용
        clientMessage?.let { copySessionAndReceiptFromMessage(it, accessor) }

        return MessageBuilder.createMessage(payload, accessor.messageHeaders)
    }

    /** 인터셉터에서 Message<*>로 넘겨도 헤더에서 sessionId 추출 (payload 타입 무관). */
    fun handleClientMessageProcessingError(clientMessage: Message<*>?, ex: Throwable): Message<ByteArray>? {
        val byteArrayMessage = clientMessage as? Message<ByteArray>
        if (byteArrayMessage != null) return handleClientMessageProcessingError(byteArrayMessage, ex)
        if (clientMessage == null) return handleClientMessageProcessingError(null, ex)
        val domainException = resolveDomainException(ex) ?: return null
        log.info("STOMP 메시지 처리 오류 → ERROR 프레임 반환: code={}, message={}", domainException.error.code, domainException.error.message)
        val payload = objectMapper.writeValueAsBytes(
            mapOf("code" to domainException.error.code, "message" to domainException.error.message),
        )
        val accessor = StompHeaderAccessor.create(StompCommand.ERROR)
        accessor.message = domainException.message
        copySessionAndReceiptFromMessage(clientMessage, accessor)
        return MessageBuilder.createMessage(payload, accessor.messageHeaders)
    }

    private fun copySessionAndReceiptFromMessage(source: Message<*>, accessor: StompHeaderAccessor) {
        val sessionId = SimpMessageHeaderAccessor.getSessionId(source.headers)
        if (sessionId != null) accessor.sessionId = sessionId
        val clientAccessor = StompHeaderAccessor.wrap(source)
        clientAccessor.receipt?.let { accessor.receiptId = it }
    }

    private fun resolveDomainException(throwable: Throwable): DomainException? {
        var t: Throwable? = throwable
        while (t != null) {
            if (t is DomainException) return t
            t = t.cause
        }
        return null
    }
}
