package site.rahoon.message.monolithic.common.websocket.config.session

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionBody
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionBodyBuilder
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionController
import java.time.Duration
import java.time.LocalDateTime

/**
 * Heartbeat 주기([HEARTBEAT_INTERVAL_MS])마다 등록된 세션의 TTL(만료)을 검사하고,
 * 만료된 세션에 대해 ERROR 프레임을 보낸 뒤 레지스트리에서 제거한다.
 *
 * - WebSocketConfig.HEARTBEAT_INTERVAL_MS와 동일 주기로 실행.
 * - [WebSocketSessionAuthInfoRegistry]에 등록된 (sessionId, authInfo)만 검사.
 */
@Component
class WebSocketSessionExpiryHeartbeatTask(
    private val sessionAuthInfoRegistry: WebSocketSessionAuthInfoRegistry,
    private val exceptionBodyBuilder: WebSocketExceptionBodyBuilder,
    private val exceptionController: WebSocketExceptionController,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        /** WebSocketConfig.HEARTBEAT_INTERVAL_MS와 동일 값. */
        private const val HEARTBEAT_INTERVAL_MS = 10000L
    }

    private val taskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 1
        setThreadNamePrefix("ws-expiry-")
        initialize()
    }

    @EventListener(ApplicationReadyEvent::class)
    fun scheduleExpiryCheck() {
        taskScheduler.scheduleAtFixedRate(
            this::checkExpiredSessions,
            Duration.ofMillis(HEARTBEAT_INTERVAL_MS),
        )
    }

    private fun checkExpiredSessions() {
        val now = LocalDateTime.now()
        val snapshot = sessionAuthInfoRegistry.snapshot()
        for ((sessionId, authInfo) in snapshot) {
            if (authInfo.expiresAt.isBefore(now)) {
                log.warn(
                    "세션 만료(heartbeat 검사)로 연결 종료: userId={}, sessionId={}, wsSessionId={}",
                    authInfo.userId,
                    authInfo.sessionId,
                    sessionId,
                )
                val body = buildExpiredBody(sessionId, authInfo)
                try {
                    exceptionController.sendErrorFrame(body)
                } catch (e: Exception) {
                    log.debug("ERROR 프레임 전송 실패(연결 이미 끊김 등): sessionId={}", sessionId, e)
                }
                sessionAuthInfoRegistry.unregister(sessionId)
            }
        }
    }

    private fun buildExpiredBody(
        websocketSessionId: String,
        authInfo: CommonAuthInfo,
    ): WebSocketExceptionBody {
        val domainException = DomainException(
            CommonError.UNAUTHORIZED,
            mapOf(
                "reason" to "Session expired",
                "expiresAt" to authInfo.expiresAt.toString(),
            ),
        )
        return exceptionBodyBuilder.fromDomainException(
            domainException,
            websocketSessionId = websocketSessionId,
            receiptId = null,
            requestDestination = null,
        )
    }
}
