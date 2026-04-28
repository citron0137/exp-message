package site.rahoon.message.monolithic.presentation.websocket.widget

import java.time.LocalDateTime

data class WidgetWebSocketSession(
    val publicKey: String,
    val origin: String,
    val visitorSessionToken: String,
    val channelId: String,
    val visitorId: String,
    val visitorSessionId: String,
    val expiresAt: LocalDateTime,
)
