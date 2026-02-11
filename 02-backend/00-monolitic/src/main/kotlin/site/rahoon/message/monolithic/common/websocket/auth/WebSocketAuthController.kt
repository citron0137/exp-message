package site.rahoon.message.monolithic.common.websocket.auth

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.websocket.annotation.WebsocketSend

/**
 * WebSocket auth 큐 메시지 전송.
 *
 * - [sendToAuthQueue]: [@WebsocketSend]로 `/queue/session/{websocketSessionId}/auth` 전송
 *   (body.websocketSessionId가 있을 때만, null 반환 시 전송 안 함)
 *
 * WebSocketExceptionController와 동일한 패턴. [WebsocketSendAspect]가 전송 수행.
 */
@Component
class WebSocketAuthController {

    /**
     * [body]를 MESSAGE command로 `/queue/session/{websocketSessionId}/auth`에 전송한다.
     * body.websocketSessionId가 비어 있지 않을 때만 body를 반환하여 [@WebsocketSend] Aspect가 전송.
     */
    @WebsocketSend("/queue/session/{websocketSessionId}/auth")
    fun sendToAuthQueue(body: WebSocketAuthBody): WebSocketAuthBody? =
        body.takeIf { it.websocketSessionId.isNotBlank() }
}
