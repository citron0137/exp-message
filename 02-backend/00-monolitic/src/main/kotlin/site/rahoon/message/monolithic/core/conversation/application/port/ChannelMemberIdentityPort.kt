package site.rahoon.message.monolithic.core.conversation.application.port

interface ChannelMemberIdentityPort {
    /**
     * Creates or loads a channel user identity.
     */
    fun createOrLoadChannelMember(command: CreateOrLoadChannelMemberIdentityCommand): ChannelMemberIdentity
}

data class CreateOrLoadChannelMemberIdentityCommand(
    val email: String,
    val nickname: String,
)

data class ChannelMemberIdentity(
    val userId: String,
    val email: String,
    val nickname: String,
    val temporaryPassword: String?,
    val created: Boolean,
)
