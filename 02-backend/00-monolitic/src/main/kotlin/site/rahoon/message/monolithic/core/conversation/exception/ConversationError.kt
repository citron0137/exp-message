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
    CHANNEL_NOT_ACTIVE(
        code = "CONVERSATION_CHANNEL_NOT_ACTIVE",
        userMessage = "활성화된 채널이 아닙니다.",
        developerMessage = "The conversation channel is not active.",
        category = ErrorCategory.CONFLICT,
    ),
    CHANNEL_INTEGRATION_NOT_FOUND(
        code = "CONVERSATION_CHANNEL_INTEGRATION_NOT_FOUND",
        userMessage = "채널 연동을 찾을 수 없습니다.",
        developerMessage = "The channel integration was not found.",
        category = ErrorCategory.NOT_FOUND,
    ),
    CHANNEL_INTEGRATION_ALREADY_EXISTS(
        code = "CONVERSATION_CHANNEL_INTEGRATION_ALREADY_EXISTS",
        userMessage = "이미 활성화된 채널 연동이 있습니다.",
        developerMessage = "An active channel integration already exists for the channel and type.",
        category = ErrorCategory.CONFLICT,
    ),
    CHANNEL_INTEGRATION_DISABLED(
        code = "CONVERSATION_CHANNEL_INTEGRATION_DISABLED",
        userMessage = "비활성화된 채널 연동입니다.",
        developerMessage = "The channel integration is disabled.",
        category = ErrorCategory.FORBIDDEN,
    ),
    CHANNEL_INTEGRATION_ORIGIN_DENIED(
        code = "CONVERSATION_CHANNEL_INTEGRATION_ORIGIN_DENIED",
        userMessage = "허용되지 않은 요청 출처입니다.",
        developerMessage = "The request origin is not allowed for the channel integration.",
        category = ErrorCategory.FORBIDDEN,
    ),
    INVALID_WIDGET_ORIGIN(
        code = "CONVERSATION_INVALID_WIDGET_ORIGIN",
        userMessage = "요청 출처가 올바르지 않습니다.",
        developerMessage = "The widget request origin is invalid.",
        category = ErrorCategory.BAD_REQUEST,
    ),
    VISITOR_SESSION_NOT_FOUND(
        code = "CONVERSATION_VISITOR_SESSION_NOT_FOUND",
        userMessage = "방문자 세션을 찾을 수 없습니다.",
        developerMessage = "The visitor session was not found.",
        category = ErrorCategory.UNAUTHORIZED,
    ),
    VISITOR_SESSION_EXPIRED(
        code = "CONVERSATION_VISITOR_SESSION_EXPIRED",
        userMessage = "방문자 세션이 만료되었습니다.",
        developerMessage = "The visitor session has expired.",
        category = ErrorCategory.UNAUTHORIZED,
    ),
    VISITOR_NOT_FOUND(
        code = "CONVERSATION_VISITOR_NOT_FOUND",
        userMessage = "방문자를 찾을 수 없습니다.",
        developerMessage = "The visitor was not found.",
        category = ErrorCategory.NOT_FOUND,
    ),
    CHANNEL_CONVERSATION_NOT_FOUND(
        code = "CONVERSATION_CHANNEL_CONVERSATION_NOT_FOUND",
        userMessage = "대화를 찾을 수 없습니다.",
        developerMessage = "The channel conversation was not found.",
        category = ErrorCategory.NOT_FOUND,
    ),
    CHANNEL_CONVERSATION_NOT_APPENDABLE(
        code = "CONVERSATION_CHANNEL_CONVERSATION_NOT_APPENDABLE",
        userMessage = "현재 대화에 메시지를 보낼 수 없습니다.",
        developerMessage = "The channel conversation cannot accept a visitor message.",
        category = ErrorCategory.CONFLICT,
    ),
    CHANNEL_CONVERSATION_NOT_VIEWABLE(
        code = "CONVERSATION_CHANNEL_CONVERSATION_NOT_VIEWABLE",
        userMessage = "현재 대화를 조회할 수 없습니다.",
        developerMessage = "The channel conversation cannot be viewed by the visitor.",
        category = ErrorCategory.FORBIDDEN,
    ),
    CHANNEL_CONVERSATION_REPLY_NOT_ALLOWED(
        code = "CONVERSATION_CHANNEL_CONVERSATION_REPLY_NOT_ALLOWED",
        userMessage = "현재 대화에 답장할 수 없습니다.",
        developerMessage = "The channel conversation cannot accept an admin reply.",
        category = ErrorCategory.FORBIDDEN,
    ),
    INVALID_MESSAGE_CONTENT(
        code = "CONVERSATION_INVALID_MESSAGE_CONTENT",
        userMessage = "메시지 내용을 확인해주세요.",
        developerMessage = "The message content is blank or exceeds the allowed length.",
        category = ErrorCategory.BAD_REQUEST,
    ),
    PLATFORM_ADMIN_REQUIRED(
        code = "CONVERSATION_PLATFORM_ADMIN_REQUIRED",
        userMessage = "운영 관리자 권한이 필요합니다.",
        developerMessage = "The conversation operation requires PLATFORM_ADMIN.",
        category = ErrorCategory.FORBIDDEN,
    ),
    CHANNEL_ACCESS_DENIED(
        code = "CONVERSATION_CHANNEL_ACCESS_DENIED",
        userMessage = "채널 접근 권한이 없습니다.",
        developerMessage = "The principal does not have access to the channel.",
        category = ErrorCategory.FORBIDDEN,
    ),
    CHANNEL_ADMIN_REQUIRED(
        code = "CONVERSATION_CHANNEL_ADMIN_REQUIRED",
        userMessage = "채널 관리자 권한이 필요합니다.",
        developerMessage = "The conversation operation requires PLATFORM_ADMIN or CHANNEL_ADMIN.",
        category = ErrorCategory.FORBIDDEN,
    ),
    CHANNEL_MEMBERSHIP_NOT_FOUND(
        code = "CONVERSATION_CHANNEL_MEMBERSHIP_NOT_FOUND",
        userMessage = "채널 멤버를 찾을 수 없습니다.",
        developerMessage = "The channel membership was not found.",
        category = ErrorCategory.NOT_FOUND,
    ),
    CHANNEL_MEMBERSHIP_NOT_ASSIGNABLE(
        code = "CONVERSATION_CHANNEL_MEMBERSHIP_NOT_ASSIGNABLE",
        userMessage = "이 대화에 할당할 수 없는 채널 멤버입니다.",
        developerMessage = "The channel membership cannot be assigned to the conversation.",
        category = ErrorCategory.CONFLICT,
    ),
    CHANNEL_MEMBERSHIP_ALREADY_EXISTS(
        code = "CONVERSATION_CHANNEL_MEMBERSHIP_ALREADY_EXISTS",
        userMessage = "이미 채널 멤버로 등록된 사용자입니다.",
        developerMessage = "A channel membership already exists for the user.",
        category = ErrorCategory.CONFLICT,
    ),
    CHANNEL_MEMBERSHIP_ROLE_NOT_ALLOWED(
        code = "CONVERSATION_CHANNEL_MEMBERSHIP_ROLE_NOT_ALLOWED",
        userMessage = "요청한 채널 멤버 권한을 부여할 수 없습니다.",
        developerMessage = "The requested channel membership role is not allowed for the actor.",
        category = ErrorCategory.FORBIDDEN,
    ),
    CHANNEL_MEMBERSHIP_STATUS_CHANGE_NOT_ALLOWED(
        code = "CONVERSATION_CHANNEL_MEMBERSHIP_STATUS_CHANGE_NOT_ALLOWED",
        userMessage = "채널 멤버 상태를 변경할 수 없습니다.",
        developerMessage = "The channel membership status change is not allowed.",
        category = ErrorCategory.FORBIDDEN,
    ),
    CHANNEL_MEMBERSHIP_ROLE_CHANGE_NOT_ALLOWED(
        code = "CONVERSATION_CHANNEL_MEMBERSHIP_ROLE_CHANGE_NOT_ALLOWED",
        userMessage = "채널 멤버 권한을 변경할 수 없습니다.",
        developerMessage = "The channel membership role change is not allowed.",
        category = ErrorCategory.FORBIDDEN,
    ),
    CHANNEL_MEMBERSHIP_LAST_ADMIN_REQUIRED(
        code = "CONVERSATION_CHANNEL_MEMBERSHIP_LAST_ADMIN_REQUIRED",
        userMessage = "채널에는 최소 한 명의 활성 관리자가 필요합니다.",
        developerMessage = "At least one active CHANNEL_ADMIN membership must remain in the channel.",
        category = ErrorCategory.CONFLICT,
    ),
    CHANNEL_MEMBERSHIP_SELF_CHANGE_NOT_ALLOWED(
        code = "CONVERSATION_CHANNEL_MEMBERSHIP_SELF_CHANGE_NOT_ALLOWED",
        userMessage = "자기 자신의 채널 멤버 권한 또는 상태는 변경할 수 없습니다.",
        developerMessage = "A channel admin cannot change their own channel membership role or status.",
        category = ErrorCategory.CONFLICT,
    ),
    CHANNEL_CONVERSATION_STATUS_CHANGE_NOT_ALLOWED(
        code = "CONVERSATION_CHANNEL_CONVERSATION_STATUS_CHANGE_NOT_ALLOWED",
        userMessage = "대화 상태를 변경할 수 없습니다.",
        developerMessage = "The channel conversation status transition is not allowed.",
        category = ErrorCategory.CONFLICT,
    ),
    INVALID_ADMIN_INBOX_CURSOR(
        code = "CONVERSATION_INVALID_ADMIN_INBOX_CURSOR",
        userMessage = "대화 목록 커서가 올바르지 않습니다.",
        developerMessage = "The admin inbox cursor is invalid.",
        category = ErrorCategory.BAD_REQUEST,
    ),
    INVALID_ADMIN_INBOX_FILTER(
        code = "CONVERSATION_INVALID_ADMIN_INBOX_FILTER",
        userMessage = "대화 목록 필터가 올바르지 않습니다.",
        developerMessage = "The admin inbox filter is invalid.",
        category = ErrorCategory.BAD_REQUEST,
    ),
    ;

    override val boundedContext: BoundedContext = BoundedContext.CONVERSATION
}
