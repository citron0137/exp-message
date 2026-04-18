package site.rahoon.message.monolithic.core.conversation.infrastructure.iam

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMemberIdentity
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMemberIdentityPort
import site.rahoon.message.monolithic.core.conversation.application.port.CreateOrLoadChannelMemberIdentityCommand
import site.rahoon.message.monolithic.core.iam.identity.application.facade.CreateOrLoadChannelUserCommand
import site.rahoon.message.monolithic.core.iam.identity.application.facade.IdentityFacade

@Component
class ChannelMemberIdentityAdapter(
    private val identityFacade: IdentityFacade,
) : ChannelMemberIdentityPort {
    /**
     * Creates or loads a channel member identity through the IAM facade.
     */
    override fun createOrLoadChannelMember(command: CreateOrLoadChannelMemberIdentityCommand): ChannelMemberIdentity {
        val result =
            identityFacade.createOrLoadChannelUser(
                CreateOrLoadChannelUserCommand(
                    email = command.email,
                    nickname = command.nickname,
                ),
            )
        return ChannelMemberIdentity(
            userId = result.userId,
            email = result.email,
            nickname = result.nickname,
            temporaryPassword = result.temporaryPassword,
            created = result.created,
        )
    }
}
