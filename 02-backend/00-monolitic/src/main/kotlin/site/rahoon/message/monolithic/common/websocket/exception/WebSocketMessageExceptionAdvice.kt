package site.rahoon.message.monolithic.common.websocket.exception

import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.web.bind.annotation.ControllerAdvice
import site.rahoon.message.monolithic.common.domain.DomainException

/**
 * @MessageMapping 메서드에서 던진 예외를 전역 처리.
 *
 * WebSocketAnnotationMethodMessageHandler가 @ControllerAdvice + @MessageExceptionHandler를 지원하므로
 * 모든 @MessageMapping 컨트롤러에서 발생한 예외를 여기서 처리한다.
 * [DomainException]은 code/message/details 그대로, 그 외 [Exception]은 [CommonError.SERVER_ERROR]로 body 생성.
 *
 * **전송 방식 (기본):** exception 큐만 전송. ERROR 프레임(sendErrorFrame)은 기본적으로 호출하지 않는다.
 * ERROR 프레임까지 보내고 싶으면 아래 주석을 해제하면 된다.
 */
@ControllerAdvice
class WebSocketMessageExceptionAdvice(
    private val exceptionBodyBuilder: WebSocketExceptionBodyBuilder,
    private val exceptionController: WebSocketExceptionController,
) {

    @MessageExceptionHandler(DomainException::class)
    fun handleDomainException(ex: DomainException, message: Message<*>) {
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
    fun handleException(ex: Exception, message: Message<*>) {
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
