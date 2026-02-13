package site.rahoon.message.monolithic.common.websocket.config.exception

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
 * STOMP 인바운드 채널·인터셉터 단계에서 발생한 예외만 ERROR 프레임으로 반환하는 핸들러.
 *
 * Spring은 **@MessageMapping으로 디스패치되기 전**(clientInboundChannel·인터셉터) 예외만
 * 이 핸들러로 넘긴다. CONNECT 실패, [WebSocketSessionExpiryInterceptor] 만료, 구독 거부 등이 해당.
 *
 * **@MessageMapping 메서드 내부**에서 던진 예외는 이 핸들러로 오지 않는다.
 * 그 경우는 [WebSocketMessageExceptionAdvice](@MessageExceptionHandler)에서 처리한다.
 *
 * [WebSocketExceptionBodyBuilder]로 예외를 [WebSocketExceptionBody]로 변환하고,
 * [WebSocketExceptionController.buildErrorMessage]로 ERROR 메시지를 만들어 반환한다.
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
