package site.rahoon.message.monolithic.common.websocket.config

import org.slf4j.LoggerFactory
import org.springframework.lang.Nullable
import org.springframework.messaging.Message
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionBodyBuilder
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionController

/**
 * STOMP 프로토콜 처리 중 예외 시 ERROR 프레임을 반환하는 핸들러.
 *
 * [WebSocketExceptionBodyBuilder]로 예외를 [WebSocketExceptionBody]로 변환하고,
 * [WebSocketExceptionController]로 ERROR 메시지를 만들어 반환한다.
 * DomainException이 아니면 기본 [StompSubProtocolErrorHandler] 동작(스택 트레이스 등)을 따른다.
 *
 * **호출 시점:** clientInboundChannel.send() 이전·동기 처리에서 발생한 예외만 전달된다.
 * @MessageMapping 핸들러에서 던진 예외는 [WebSocketMessageExceptionAdvice]에서 처리한다.
 */
@Component
class WebSocketExceptionStompSubProtocolErrorHandler(
    private val exceptionBodyBuilder: WebSocketExceptionBodyBuilder,
    private val exceptionController: WebSocketExceptionController,
) : StompSubProtocolErrorHandler() {

    private val log = LoggerFactory.getLogger(javaClass)

    @Nullable
    override fun handleClientMessageProcessingError(
        clientMessage: Message<ByteArray>?,
        ex: Throwable,
    ): Message<ByteArray>? {
        val stompAccessor = clientMessage?.let { StompHeaderAccessor.wrap(it) }
        val websocketSessionId = clientMessage?.let { SimpMessageHeaderAccessor.getSessionId(it.headers) }
        val receiptId = stompAccessor?.receipt
        val requestDestination = stompAccessor?.destination
        val body = exceptionBodyBuilder.build(ex, websocketSessionId, receiptId, requestDestination)
        log.info("STOMP 메시지 처리 오류 → ERROR 프레임 반환: code={}, message={}", body.code, body.message)
        return exceptionController.buildErrorMessage(body)
    }
}
