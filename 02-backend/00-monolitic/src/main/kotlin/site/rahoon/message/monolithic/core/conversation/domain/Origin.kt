package site.rahoon.message.monolithic.core.conversation.domain

import java.net.URI

data class Origin(
    val value: String,
) {
    companion object {
        /**
         * Parses and normalizes an origin string to scheme, host, and optional port.
         */
        fun parse(rawOrigin: String): Origin? =
            runCatching {
                val trimmed = rawOrigin.trim()
                require(trimmed.isNotBlank())

                val uri = URI(trimmed)
                val scheme = requireNotNull(uri.scheme).lowercase()
                val host = requireNotNull(uri.host).lowercase()
                require(scheme == "http" || scheme == "https")

                val port =
                    uri.port
                        .takeIf { it >= 0 }
                        ?.let { ":$it" }
                        .orEmpty()
                Origin("$scheme://$host$port")
            }.getOrNull()
    }
}
