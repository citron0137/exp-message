package site.rahoon.message.monolithic.common.websocket.config.session

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket 세션별 [CommonAuthInfo]를 보관한다.
 *
 * - CONNECT 성공 시 등록, DISCONNECT 시 해제.
 * - Heartbeat 주기에서 세션 TTL(만료) 검사 시 사용.
 */
@Component
class WebSocketSessionAuthInfoRegistry {
    private val sessionIdToAuthInfo = ConcurrentHashMap<String, CommonAuthInfo>()

    fun register(
        sessionId: String,
        authInfo: CommonAuthInfo,
    ) {
        sessionIdToAuthInfo[sessionId] = authInfo
    }

    fun unregister(sessionId: String?) {
        sessionId?.let { sessionIdToAuthInfo.remove(it) }
    }

    /**
     * 등록된 모든 (sessionId, authInfo) 쌍을 반환한다. 스냅샷.
     */
    fun snapshot(): Map<String, CommonAuthInfo> = sessionIdToAuthInfo.toMap()
}
