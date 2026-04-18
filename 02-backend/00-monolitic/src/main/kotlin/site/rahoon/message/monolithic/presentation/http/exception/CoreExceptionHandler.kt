package site.rahoon.message.monolithic.presentation.http.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import site.rahoon.message.monolithic.core.shared.error.CoreException
import site.rahoon.message.monolithic.core.shared.error.ErrorCategory
import site.rahoon.message.monolithic.presentation.http.shared.ApiResponse
import site.rahoon.message.monolithic.presentation.http.shared.ErrorBody
import java.time.ZonedDateTime

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
class CoreExceptionHandler {
    /**
     * Handles core exceptions with the new presentation response shape.
     */
    @ExceptionHandler(CoreException::class)
    fun handleCoreException(
        exception: CoreException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val response =
            ApiResponse.error<Nothing>(
                ErrorBody(
                    boundedContext = exception.error.boundedContext,
                    code = exception.error.code,
                    message = exception.error.userMessage,
                    developerMessage = exception.error.developerMessage,
                    details = exception.details,
                    occurredAt = ZonedDateTime.now(),
                    path = request.requestURI,
                ),
            )
        return ResponseEntity.status(exception.error.category.toHttpStatus()).body(response)
    }

    /**
     * Maps a core error category to an HTTP status.
     */
    private fun ErrorCategory.toHttpStatus(): HttpStatus =
        when (this) {
            ErrorCategory.BAD_REQUEST -> HttpStatus.BAD_REQUEST
            ErrorCategory.UNAUTHORIZED -> HttpStatus.UNAUTHORIZED
            ErrorCategory.FORBIDDEN -> HttpStatus.FORBIDDEN
            ErrorCategory.NOT_FOUND -> HttpStatus.NOT_FOUND
            ErrorCategory.CONFLICT -> HttpStatus.CONFLICT
            ErrorCategory.SERVER_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR
        }
}
