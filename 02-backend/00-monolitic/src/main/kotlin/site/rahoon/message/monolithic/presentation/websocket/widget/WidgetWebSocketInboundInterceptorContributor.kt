package site.rahoon.message.monolithic.presentation.websocket.widget

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.websocket.config.WebSocketInboundInterceptorContributor

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + WidgetWebSocketInboundInterceptorContributor.ORDER_OFFSET)
class WidgetWebSocketInboundInterceptorContributor(
    private val widgetWebSocketConnectInterceptor: WidgetWebSocketConnectInterceptor,
    private val widgetWebSocketSessionExpiryInterceptor: WidgetWebSocketSessionExpiryInterceptor,
    private val widgetWebSocketSubscribeInterceptor: WidgetWebSocketSubscribeInterceptor,
) : WebSocketInboundInterceptorContributor {
    /**
     * Returns widget-specific inbound interceptors.
     */
    override fun interceptors(): List<ChannelInterceptor> =
        listOf(
            widgetWebSocketConnectInterceptor,
            widgetWebSocketSessionExpiryInterceptor,
            widgetWebSocketSubscribeInterceptor,
        )

    companion object {
        const val ORDER_OFFSET = 20
    }
}
