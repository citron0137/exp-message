package site.rahoon.message.monolithic.common.websocket.config

import org.springframework.messaging.support.ChannelInterceptor

interface WebSocketInboundInterceptorContributor {
    /**
     * Returns additional inbound channel interceptors.
     */
    fun interceptors(): List<ChannelInterceptor>
}
