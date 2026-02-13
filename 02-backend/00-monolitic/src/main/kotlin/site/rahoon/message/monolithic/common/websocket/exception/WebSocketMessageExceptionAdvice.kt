package site.rahoon.message.monolithic.common.websocket.exception

import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.web.bind.annotation.ControllerAdvice
import site.rahoon.message.monolithic.common.domain.DomainException

/**
 * @MessageMapping 메서드 내부에서 던진 예외를 전역 처리.
 *
 * Spring STOMP에서는 @MessageMapping 핸들러에서 나온 예외가
 * [WebSocketExceptionStompSubProtocolErrorHandler]로 전달되지 않고, @ControllerAdvice로만 온다.
 * 따라서 인바운드·인터셉터 단계 예외(인증/만료/구독)는 ErrorHandler가, 핸들러 내부 예외는 여기서 처리한다.
 *
 * [DomainException]은 code/message/details 그대로, 그 외 [Exception]은 [CommonError.SERVER_ERROR]로 body 생성.
 *
 * **전송 방식 (기본):** exception 큐만 전송. ERROR 프레임(sendErrorFrame)은 기본 비사용.
 * ERROR 프레임까지 보내려면 아래 주석을 해제하면 된다.
 */
@ControllerAdvice
class WebSocketMessageExceptionAdvice(
    private val exceptionBodyBuilder: WebSocketExceptionBodyBuilder,
    private val exceptionController: WebSocketExceptionController,
) {
    @MessageExceptionHandler(DomainException::class)
    fun handleDomainException(
        ex: DomainException,
        message: Message<*>,
    ) {
        val stompAccessor = StompHeaderAccessor.wrap(message)
        val sessionId = SimpMessageHeaderAccessor.getSessionId(message.headers) ?: stompAccessor.sessionId
        val receiptId = stompAccessor.receipt
        val requestDestination = stompAccessor.destination
        val body = exceptionBodyBuilder.fromDomainException(ex, sessionId, receiptId, requestDestination)
        // sendErrorFrame은 기본 비사용. ERROR 프레임까지 보내려면 주석 해제.
        // exceptionController.sendErrorFrame(body)
        exceptionController.sendToExceptionQueue(body)
    }

    @MessageExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception,
        message: Message<*>,
    ) {
        val stompAccessor = StompHeaderAccessor.wrap(message)
        val sessionId = SimpMessageHeaderAccessor.getSessionId(message.headers) ?: stompAccessor.sessionId
        val receiptId = stompAccessor.receipt
        val requestDestination = stompAccessor.destination
        val body = exceptionBodyBuilder.build(ex, sessionId, receiptId, requestDestination)
        // sendErrorFrame은 기본 비사용. ERROR 프레임까지 보내려면 주석 해제.
        // exceptionController.sendErrorFrame(body)
        exceptionController.sendToExceptionQueue(body)
    }
}
