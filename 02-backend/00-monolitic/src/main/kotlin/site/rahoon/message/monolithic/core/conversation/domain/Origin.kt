package site.rahoon.message.monolithic.core.conversation.domain

import java.net.URI

data class Origin(
    val value: String,
) {
    companion object {
        /**
         * Parses and normalizes an origin string to scheme, host, and optional port.
         */
        fun parse(rawOrigin: String): Origin? {
            val trimmed = rawOrigin.trim()
            if (trimmed.isBlank()) {
                return null
            }
            val uri =
                runCatching { URI(trimmed) }
                    .getOrNull()
                    ?: return null
            val scheme = uri.scheme?.lowercase() ?: return null
            val host = uri.host?.lowercase() ?: return null
            if (scheme != "http" && scheme != "https") {
                return null
            }
            val port =
                uri.port
                    .takeIf { it >= 0 }
                    ?.let { ":$it" }
                    .orEmpty()
            return Origin("$scheme://$host$port")
        }
    }
}
