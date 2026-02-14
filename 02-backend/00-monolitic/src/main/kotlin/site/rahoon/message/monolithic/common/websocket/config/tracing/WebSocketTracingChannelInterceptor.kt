package site.rahoon.message.monolithic.common.websocket.config.tracing

import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component

/**
 * clientInboundChannel용 ChannelInterceptor.
 *
 * WebSocket 메시지 처리 스레드에는 HTTP 요청 context가 없어 traceId/spanId가 비어 있다.
 * preSend에서 메시지별 span을 생성·scope에 넣어 MDC에 traceId/spanId를 설정하고,
 * afterSendCompletion에서 scope·span을 정리한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class WebSocketTracingChannelInterceptor(
    private val tracer: Tracer,
) : ChannelInterceptor {

    private val spanHolder = ThreadLocal<Span?>()
    private val scopeHolder = ThreadLocal<Tracer.SpanInScope?>()

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val span = tracer.nextSpan().name("websocket-inbound").start()
        spanHolder.set(span)
        scopeHolder.set(tracer.withSpan(span))
        return message
    }

    override fun afterSendCompletion(
        message: Message<*>,
        channel: MessageChannel,
        sent: Boolean,
        ex: Exception?,
    ) {
        try {
            scopeHolder.get()?.close()
            spanHolder.get()?.end()
        } finally {
            scopeHolder.remove()
            spanHolder.remove()
        }
    }
}
