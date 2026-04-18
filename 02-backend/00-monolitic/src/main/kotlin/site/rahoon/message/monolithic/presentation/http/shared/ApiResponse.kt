package site.rahoon.message.monolithic.presentation.http.shared

import site.rahoon.message.monolithic.core.shared.error.BoundedContext
import java.time.ZonedDateTime

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorBody? = null,
) {
    companion object {
        /**
         * Creates a successful API response.
         */
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        /**
         * Creates a failed API response.
         */
        fun <T> error(error: ErrorBody): ApiResponse<T> = ApiResponse(success = false, error = error)
    }
}

data class ErrorBody(
    val boundedContext: BoundedContext,
    val code: String,
    val message: String,
    val developerMessage: String,
    val details: Map<String, Any?>,
    val occurredAt: ZonedDateTime,
    val path: String,
)
