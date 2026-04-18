package site.rahoon.message.monolithic.core.conversation.exception

import site.rahoon.message.monolithic.core.shared.error.BoundedContext
import site.rahoon.message.monolithic.core.shared.error.CoreError
import site.rahoon.message.monolithic.core.shared.error.ErrorCategory

enum class ConversationError(
    override val code: String,
    override val userMessage: String,
    override val developerMessage: String,
    override val category: ErrorCategory,
) : CoreError {
    CHANNEL_NOT_FOUND(
        code = "CONVERSATION_CHANNEL_NOT_FOUND",
        userMessage = "채널을 찾을 수 없습니다.",
        developerMessage = "The conversation channel was not found.",
        category = ErrorCategory.NOT_FOUND,
    ),
    PLATFORM_ADMIN_REQUIRED(
        code = "CONVERSATION_PLATFORM_ADMIN_REQUIRED",
        userMessage = "운영 관리자 권한이 필요합니다.",
        developerMessage = "The conversation operation requires PLATFORM_ADMIN.",
        category = ErrorCategory.FORBIDDEN,
    ),
    ;

    override val boundedContext: BoundedContext = BoundedContext.CONVERSATION
}
