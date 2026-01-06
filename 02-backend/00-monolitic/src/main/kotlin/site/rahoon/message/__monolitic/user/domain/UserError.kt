package site.rahoon.message.__monolitic.user.domain

import site.rahoon.message.__monolitic.common.domain.DomainError

enum class UserError(
    override val code: String,
    override val message: String
) : DomainError {
    USER_NOT_FOUND("USER_001", "User not found"),
    EMAIL_ALREADY_EXISTS("USER_002", "Email already exists"),
    INVALID_EMAIL("USER_003", "Invalid email format"),
    INVALID_PASSWORD("USER_004", "Invalid password"),
    INVALID_NICKNAME("USER_005", "Invalid nickname")
}

