package site.rahoon.message.monolithic.core.iam.exception

import site.rahoon.message.monolithic.core.shared.error.BoundedContext
import site.rahoon.message.monolithic.core.shared.error.CoreError
import site.rahoon.message.monolithic.core.shared.error.ErrorCategory

enum class AccessError(
    override val code: String,
    override val userMessage: String,
    override val developerMessage: String,
    override val category: ErrorCategory,
) : CoreError {
    INVALID_CREDENTIALS(
        code = "ACCESS_INVALID_CREDENTIALS",
        userMessage = "이메일 또는 비밀번호가 올바르지 않습니다.",
        developerMessage = "The supplied login credentials are invalid.",
        category = ErrorCategory.UNAUTHORIZED,
    ),
    TOKEN_EXPIRED(
        code = "ACCESS_TOKEN_EXPIRED",
        userMessage = "로그인이 만료되었습니다.",
        developerMessage = "The access token has expired.",
        category = ErrorCategory.UNAUTHORIZED,
    ),
    INVALID_TOKEN(
        code = "ACCESS_INVALID_TOKEN",
        userMessage = "인증 토큰이 올바르지 않습니다.",
        developerMessage = "The access token is invalid.",
        category = ErrorCategory.UNAUTHORIZED,
    ),
    REFRESH_TOKEN_NOT_FOUND(
        code = "ACCESS_REFRESH_TOKEN_NOT_FOUND",
        userMessage = "다시 로그인해 주세요.",
        developerMessage = "The refresh token was not found.",
        category = ErrorCategory.UNAUTHORIZED,
    ),
    REFRESH_TOKEN_EXPIRED(
        code = "ACCESS_REFRESH_TOKEN_EXPIRED",
        userMessage = "다시 로그인해 주세요.",
        developerMessage = "The refresh token has expired.",
        category = ErrorCategory.UNAUTHORIZED,
    ),
    AUTHORIZATION_HEADER_MISSING(
        code = "ACCESS_AUTHORIZATION_HEADER_MISSING",
        userMessage = "로그인이 필요합니다.",
        developerMessage = "The Authorization header is missing.",
        category = ErrorCategory.UNAUTHORIZED,
    ),
    PLATFORM_ADMIN_REQUIRED(
        code = "ACCESS_PLATFORM_ADMIN_REQUIRED",
        userMessage = "운영 관리자 권한이 필요합니다.",
        developerMessage = "The operation requires PLATFORM_ADMIN.",
        category = ErrorCategory.FORBIDDEN,
    ),
    ;

    override val boundedContext: BoundedContext = BoundedContext.IAM
}
