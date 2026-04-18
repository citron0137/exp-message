package site.rahoon.message.monolithic.presentation.http.exception

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import site.rahoon.message.monolithic.core.shared.error.BoundedContext
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
     * Handles request body validation failures with the same API error envelope as core errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val fieldErrors =
            exception.bindingResult
                .fieldErrors
                .map { it.toDetail() }
        return badRequest(
            request = request,
            code = "PRESENTATION_REQUEST_VALIDATION_FAILED",
            message = "요청 값을 확인해주세요.",
            developerMessage = "Request body validation failed.",
            details = mapOf("fields" to fieldErrors),
        )
    }

    /**
     * Handles invalid enum, number, and other request parameter binding failures.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(
        exception: MethodArgumentTypeMismatchException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> =
        badRequest(
            request = request,
            code = "PRESENTATION_REQUEST_PARAMETER_INVALID",
            message = "요청 파라미터를 확인해주세요.",
            developerMessage = "Request parameter binding failed.",
            details =
                mapOf(
                    "name" to exception.name,
                    "value" to exception.value,
                    "requiredType" to exception.requiredType?.simpleName,
                ),
        )

    /**
     * Handles malformed JSON bodies and invalid request body enum values.
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadableException(
        exception: HttpMessageNotReadableException,
        request: HttpServletRequest,
    ): ResponseEntity<ApiResponse<Nothing>> =
        badRequest(
            request = request,
            code = "PRESENTATION_REQUEST_BODY_INVALID",
            message = "요청 본문을 확인해주세요.",
            developerMessage = "Request body could not be read.",
            details = mapOf("reason" to (exception.mostSpecificCause.message ?: exception.message)),
        )

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

    /**
     * Builds a presentation-level bad request response.
     */
    private fun badRequest(
        request: HttpServletRequest,
        code: String,
        message: String,
        developerMessage: String,
        details: Map<String, Any?>,
    ): ResponseEntity<ApiResponse<Nothing>> {
        val response =
            ApiResponse.error<Nothing>(
                ErrorBody(
                    boundedContext = BoundedContext.PRESENTATION,
                    code = code,
                    message = message,
                    developerMessage = developerMessage,
                    details = details,
                    occurredAt = ZonedDateTime.now(),
                    path = request.requestURI,
                ),
            )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    /**
     * Maps one field validation error to a stable response detail.
     */
    private fun FieldError.toDetail(): Map<String, Any?> =
        mapOf(
            "field" to field,
            "rejectedValue" to rejectedValue,
            "message" to (defaultMessage ?: "Invalid value"),
        )
}
