package site.rahoon.message.monolithic.core.iam.exception

import site.rahoon.message.monolithic.core.shared.error.BoundedContext
import site.rahoon.message.monolithic.core.shared.error.CoreError
import site.rahoon.message.monolithic.core.shared.error.ErrorCategory

enum class IdentityError(
    override val code: String,
    override val userMessage: String,
    override val developerMessage: String,
    override val category: ErrorCategory,
) : CoreError {
    USER_NOT_FOUND(
        code = "IDENTITY_USER_NOT_FOUND",
        userMessage = "사용자를 찾을 수 없습니다.",
        developerMessage = "The identity user was not found.",
        category = ErrorCategory.NOT_FOUND,
    ),
    EMAIL_ALREADY_USED(
        code = "IDENTITY_EMAIL_ALREADY_USED",
        userMessage = "이미 사용 중인 이메일입니다.",
        developerMessage = "The identity email is already used.",
        category = ErrorCategory.CONFLICT,
    ),
    INVALID_ROLE(
        code = "IDENTITY_INVALID_ROLE",
        userMessage = "사용자 역할이 올바르지 않습니다.",
        developerMessage = "The identity role is invalid.",
        category = ErrorCategory.BAD_REQUEST,
    ),
    ;

    override val boundedContext: BoundedContext = BoundedContext.IAM
}
