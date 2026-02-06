package site.rahoon.message.monolithic.common.websocket.exception

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Lazy
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.websocket.annotation.WebsocketSend

/**
 * [WebSocketExceptionBody]를 받아 클라이언트에 전달한다.
 *
 * - [buildErrorMessage]: ERROR 메시지를 만들어 반환 (호출 측에서 전송)
 * - [sendErrorFrame]: ERROR command로 [clientOutboundChannel] 전송
 * - [sendToExceptionQueue]: [@WebsocketSend]로 `/queue/session/{websocketSessionId}/exception` 전송
 *   (body.websocketSessionId가 있을 때만, null 반환 시 전송 안 함)
 *
 * [clientOutboundChannel]은 [@Lazy]로 주입하여 순환 의존성을 끊는다.
 */
@Component
class WebSocketExceptionController(
    private val objectMapper: ObjectMapper,
    @Qualifier("clientOutboundChannel") @Lazy private val clientOutboundChannel: MessageChannel,
) {

    /**
     * [body]로 ERROR command 메시지를 만들어 [clientOutboundChannel]로 전송한다.
     * (@MessageMapping 예외 시 인터셉터에서 호출)
     */
    fun sendErrorFrame(body: WebSocketExceptionBody) {
        val errorMessage = buildErrorMessage(body)
        try {
            clientOutboundChannel.send(errorMessage)
        } catch (e: Exception) {
            // 전송 실패 시 로그만 (연결 끊김 등)
        }
    }

    /**
     * [body]를 MESSAGE command로 `/queue/session/{websocketSessionId}/exception`에 전송한다.
     * body.websocketSessionId가 있을 때만 body를 반환하여 [@WebsocketSend] Aspect가 전송.
     */
    @WebsocketSend("/queue/session/{websocketSessionId}/exception")
    fun sendToExceptionQueue(body: WebSocketExceptionBody): WebSocketExceptionBody? {
        return body.takeIf { it.websocketSessionId != null }
    }
    
    /**
     * ERROR 메시지를 생성해 반환한다. 전송은 호출 측에서 수행.
     * (STOMP 프로토콜 에러 시 Spring이 이 메시지를 전송)
     * body의 [websocketSessionId], [receiptId]로 라우팅·헤더를 설정한다.
     */
    fun buildErrorMessage(body: WebSocketExceptionBody): Message<ByteArray> {
        val map = mutableMapOf<String, Any>("code" to body.code, "message" to body.message)
        body.details?.let { map["details"] = it }
        body.occurredAt?.let { map["occurredAt"] = it }
        body.websocketSessionId?.let { map["websocketSessionId"] = it }
        body.receiptId?.let { map["receiptId"] = it }
        body.requestDestination?.let { map["requestDestination"] = it }
        val payload = objectMapper.writeValueAsBytes(map)
        val accessor = StompHeaderAccessor.create(StompCommand.ERROR)
        accessor.message = body.message
        body.websocketSessionId?.let { accessor.sessionId = it }
        body.receiptId?.let { accessor.receiptId = it }
        return MessageBuilder.createMessage(payload, accessor.messageHeaders)
    }
}
