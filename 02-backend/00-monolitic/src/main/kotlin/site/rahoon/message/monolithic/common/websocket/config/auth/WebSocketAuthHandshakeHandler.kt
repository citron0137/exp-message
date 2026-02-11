package site.rahoon.message.monolithic.common.websocket.config.auth

import org.springframework.http.server.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.support.DefaultHandshakeHandler
import org.springframework.web.util.UriComponentsBuilder
import java.security.Principal

/**
 * WebSocket Handshake 시 Principal은 설정하지 않고, 토큰만 세션에 넣습니다.
 *
 * - 쿼리: `access_token`
 * - 헤더: `Authorization` (Bearer)
 *
 * 둘 다 확인하여 있으면 세션 attributes에 저장. 인증·Principal 설정은 STOMP CONNECT 시
 * [WebSocketConnectInterceptor]에서 수행합니다.
 */
@Component
class WebSocketAuthHandshakeHandler : DefaultHandshakeHandler() {
    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: MutableMap<String, Any>,
    ): Principal? {
        val tokenFromQuery =
            UriComponentsBuilder
                .fromUri(request.uri)
                .build()
                .queryParams
                .getFirst("access_token")
        val tokenFromHeader = request.headers.getFirst("Authorization")
        val token =
            tokenFromQuery?.takeIf { it.isNotBlank() } ?: tokenFromHeader?.takeIf { it.isNotBlank() }
        token?.let { attributes[ATTR_TOKEN] = it }
        return null
    }

    companion object {
        /** CONNECT 인터셉터에서 읽는 세션/Handshake 토큰 키 */
        const val ATTR_TOKEN = "ws.token"

        /** CONNECT 검증 후 세션에 넣는 [site.rahoon.message.monolithic.common.auth.CommonAuthInfo] */
        const val ATTR_AUTH_INFO = "ws.authInfo"
    }
}
