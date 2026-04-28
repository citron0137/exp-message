package site.rahoon.message.monolithic.presentation.websocket.admin

import org.springframework.core.annotation.Order
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.websocket.config.WebSocketInboundInterceptorContributor

@Component
@Order(AdminWebSocketConnectInterceptor.ORDER)
class AdminWebSocketInboundInterceptorContributor(
    private val adminWebSocketConnectInterceptor: AdminWebSocketConnectInterceptor,
    private val adminWebSocketSubscribeInterceptor: AdminWebSocketSubscribeInterceptor,
) : WebSocketInboundInterceptorContributor {
    /**
     * Returns admin-specific inbound interceptors.
     */
    override fun interceptors(): List<ChannelInterceptor> =
        listOf(
            adminWebSocketConnectInterceptor,
            adminWebSocketSubscribeInterceptor,
        )
}
