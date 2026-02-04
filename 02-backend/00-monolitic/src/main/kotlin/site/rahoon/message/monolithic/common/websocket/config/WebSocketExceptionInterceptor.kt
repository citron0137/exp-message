package site.rahoon.message.monolithic.common.websocket.config

import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ExecutorChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionBodyBuilder
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionController

/**
 * clientInboundChannel에서 비동기로 처리되는 @MessageMapping 예외를 잡아
 * [WebSocketExceptionBodyBuilder]로 ExceptionBody를 만들고 exception 큐로만 전송한다.
 *
 * StompSubProtocolHandler.handleError()는 channel.send()가 동기적으로 예외를 던질 때만 호출되는데,
 * ExecutorSubscribableChannel은 send() 후 executor에서 처리하므로 예외가 전달되지 않는다.
 * 이 인터셉터는 afterMessageHandled(message, channel, handler, ex)로 예외를 받아
 * Builder로 body 생성 후 [WebSocketExceptionController.sendToExceptionQueue]로만 전송한다 (ERROR 프레임은 보내지 않음).
 */
@Component
class WebSocketExceptionInterceptor(
    private val exceptionBodyBuilder: WebSocketExceptionBodyBuilder,
    private val exceptionController: WebSocketExceptionController,
) : ExecutorChannelInterceptor {

    override fun afterMessageHandled(
        message: Message<*>,
        channel: MessageChannel,
        handler: MessageHandler,
        ex: Exception?,
    ) {
        if (ex == null) return
        val websocketSessionId = SimpMessageHeaderAccessor.getSessionId(message.headers)
        val receiptId = StompHeaderAccessor.wrap(message).receipt
        val body = exceptionBodyBuilder.build(ex, websocketSessionId, receiptId)
        exceptionController.sendToExceptionQueue(body)
    }
}
