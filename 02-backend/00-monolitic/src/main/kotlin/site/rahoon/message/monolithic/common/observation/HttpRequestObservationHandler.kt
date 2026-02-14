package site.rahoon.message.monolithic.common.observation

import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationHandler
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.server.observation.ServerRequestObservationContext
import org.springframework.stereotype.Component
import java.time.OffsetDateTime

/**
 * HTTP 요청 Observation(http.server.requests)의 method, path, status, duration을 MDC에 주입하여
 * 구조화 로깅 시 JSON 필드로 포함되도록 한다.
 *
 * Spring Boot가 자동 구성하는 ServerHttpObservationFilter의 Observation에 연결되어
 * traceId/spanId와 함께 로그에 출력된다.
 */
@Component
class HttpRequestObservationHandler : ObservationHandler<ServerRequestObservationContext> {

    companion object {
        private val log = LoggerFactory.getLogger(HttpRequestObservationHandler::class.java)

        private const val MDC_HTTP_METHOD = "http.method"
        private const val MDC_HTTP_PATH = "http.path"
        private const val MDC_HTTP_STATUS = "http.status_code"
        private const val MDC_HTTP_DURATION_MS = "http.duration_ms"
        private const val MDC_HTTP_START_TIME = "http.start_time"
        private const val MDC_HTTP_END_TIME = "http.end_time"

        private val startTime = ThreadLocal.withInitial { 0L }
    }

    override fun supportsContext(context: Observation.Context): Boolean =
        context is ServerRequestObservationContext

    override fun onStart(context: ServerRequestObservationContext) {
        startTime.set(System.nanoTime())
    }

    override fun onScopeOpened(context: ServerRequestObservationContext) {
        val request = context.carrier
        MDC.put(MDC_HTTP_METHOD, request.method)
        MDC.put(MDC_HTTP_PATH, request.requestURI)
        MDC.put(MDC_HTTP_START_TIME, OffsetDateTime.now().toString())
    }

    override fun onScopeClosed(context: ServerRequestObservationContext) {
        val durationMs = (System.nanoTime() - startTime.get()) / 1_000_000
        val response = context.response

        MDC.put(MDC_HTTP_END_TIME, OffsetDateTime.now().toString())
        if (response != null) {
            MDC.put(MDC_HTTP_STATUS, response.status.toString())
        }
        MDC.put(MDC_HTTP_DURATION_MS, durationMs.toString())

        log.debug(
            "Request completed: {} {} - {} ({}ms) - Start: {}, End: {}",
            context.carrier.method,
            context.carrier.requestURI,
            response?.status ?: "?",
            durationMs,
            MDC.get(MDC_HTTP_START_TIME),
            MDC.get(MDC_HTTP_END_TIME),
        )

        removeMdcKeys()
        startTime.remove()
    }

    private fun removeMdcKeys() {
        MDC.remove(MDC_HTTP_METHOD)
        MDC.remove(MDC_HTTP_PATH)
        MDC.remove(MDC_HTTP_STATUS)
        MDC.remove(MDC_HTTP_DURATION_MS)
        MDC.remove(MDC_HTTP_START_TIME)
        MDC.remove(MDC_HTTP_END_TIME)
    }
}
