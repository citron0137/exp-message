package site.rahoon.message.monolithic.core.conversation.application.query

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.util.Base64

data class AdminInboxCursor(
    val activityAt: LocalDateTime,
    val id: String,
) {
    /**
     * Encodes this cursor as Base64Url JSON.
     */
    fun encode(): String =
        Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(objectMapper.writeValueAsString(this).toByteArray(StandardCharsets.UTF_8))

    companion object {
        private val objectMapper: ObjectMapper =
            ObjectMapper()
                .registerModule(JavaTimeModule())
                .registerModule(KotlinModule.Builder().build())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

        /**
         * Decodes a nullable cursor string.
         */
        fun decodeOrNull(rawCursor: String?): AdminInboxCursor? =
            rawCursor
                ?.takeIf { it.isNotBlank() }
                ?.let { decode(it) }

        /**
         * Decodes a Base64Url JSON cursor string.
         */
        fun decode(rawCursor: String): AdminInboxCursor =
            try {
                val json = String(Base64.getUrlDecoder().decode(rawCursor), StandardCharsets.UTF_8)
                val cursor = objectMapper.readValue<AdminInboxCursor>(json)
                if (cursor.id.isBlank()) {
                    throw invalidCursor(rawCursor, "blank id")
                }
                cursor
            } catch (e: ConversationException) {
                throw e
            } catch (e: Exception) {
                throw invalidCursor(rawCursor, "decode failed", e)
            }

        /**
         * Creates a cursor from the last item in a returned page.
         */
        fun from(item: AdminConversationListItemResult): AdminInboxCursor =
            AdminInboxCursor(
                activityAt = item.activityAt,
                id = item.id,
            )

        /**
         * Builds a standard invalid cursor exception.
         */
        private fun invalidCursor(
            rawCursor: String,
            reason: String,
            cause: Throwable? = null,
        ): ConversationException =
            ConversationException(
                error = ConversationError.INVALID_ADMIN_INBOX_CURSOR,
                details = mapOf("cursor" to rawCursor, "reason" to reason),
                cause = cause,
            )
    }
}
