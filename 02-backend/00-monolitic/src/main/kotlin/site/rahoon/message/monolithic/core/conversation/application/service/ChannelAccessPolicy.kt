package site.rahoon.message.monolithic.core.conversation.application.service

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal

@Service
class ChannelAccessPolicy {
    /**
     * Requires the principal to be a platform admin.
     */
    fun requirePlatformAdmin(principal: AuthenticatedPrincipal) {
        if (!principal.isPlatformAdmin()) {
            throw ConversationException(ConversationError.PLATFORM_ADMIN_REQUIRED)
        }
    }
}
