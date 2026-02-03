package site.rahoon.message.monolithic.common.websocket.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.MessageHandler
import org.springframework.messaging.support.ExecutorChannelInterceptor
import org.springframework.stereotype.Component

/**
 * clientInboundChannel에서 비동기로 처리되는 @MessageMapping 예외를 잡아
 * ERROR 메시지를 clientOutboundChannel로 보낸다.
 *
 * StompSubProtocolHandler.handleError()는 channel.send()가 동기적으로 예외를 던질 때만
 * 호출되는데, ExecutorSubscribableChannel은 send() 후 executor에서 처리하므로
 * 예외가 send() 호출자에게 전달되지 않는다. 이 인터셉터는 handler 스레드에서
 * afterMessageHandled(message, channel, handler, ex)로 예외를 받아
 * [WebSocketStompErrorHandler]로 ERROR 메시지를 만들고 clientOutboundChannel로 전송한다.
 *
 * [clientOutboundChannel]은 [@Lazy]로 주입하여 DelegatingWebSocketMessageBrokerConfiguration과의
 * 순환 의존성을 끊는다.
 */
@Component
class WebSocketClientInboundErrorInterceptor(
    private val webSocketStompErrorHandler: WebSocketStompErrorHandler,
    @Qualifier("clientOutboundChannel") @Lazy private val clientOutboundChannel: MessageChannel,
) : ExecutorChannelInterceptor {

    override fun afterMessageHandled(
        message: Message<*>,
        channel: MessageChannel,
        handler: MessageHandler,
        ex: Exception?,
    ) {
        if (ex == null) return

        // payload 타입과 무관하게 원본 메시지 전달 (헤더에서 sessionId 추출용)
        val errorMessage = webSocketStompErrorHandler.handleClientMessageProcessingError(message, ex)
            ?: return

        try {
            clientOutboundChannel.send(errorMessage)
        } catch (e: Exception) {
            // 전송 실패 시 로그만 (연결 끊김 등)
        }
    }
}
