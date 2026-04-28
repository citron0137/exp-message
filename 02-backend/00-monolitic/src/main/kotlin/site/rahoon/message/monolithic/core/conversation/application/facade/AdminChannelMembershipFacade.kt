package site.rahoon.message.monolithic.core.conversation.application.facade

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMemberIdentityPort
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.application.port.CreateOrLoadChannelMemberIdentityCommand
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AgentStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembership
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipStatus
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import java.time.LocalDateTime

@Service
class AdminChannelMembershipFacade(
    private val channelAccessPolicy: ChannelAccessPolicy,
    private val channelRepository: ChannelRepository,
    private val channelMembershipRepository: ChannelMembershipRepository,
    private val channelMemberIdentityPort: ChannelMemberIdentityPort,
) {
    /**
     * Creates a channel membership and creates or loads the backing IAM user.
     */
    @Transactional
    fun createMembership(command: CreateChannelMembershipCommand): ChannelMembershipCreationResult {
        requireCreatableRole(command.actor, command.channelId, command.role)
        requireChannelExists(command.channelId)

        val identity =
            channelMemberIdentityPort.createOrLoadChannelMember(
                CreateOrLoadChannelMemberIdentityCommand(
                    email = command.email,
                    nickname = command.nickname,
                ),
            )
        if (channelMembershipRepository.existsByChannelIdAndUserId(command.channelId, identity.userId)) {
            throw ConversationException(
                error = ConversationError.CHANNEL_MEMBERSHIP_ALREADY_EXISTS,
                details = mapOf("channelId" to command.channelId, "userId" to identity.userId),
            )
        }

        val membership =
            channelMembershipRepository.save(
                ChannelMembership.create(
                    channelId = command.channelId,
                    userId = identity.userId,
                    role = command.role,
                ),
            )
        return ChannelMembershipCreationResult(
            membership = ChannelMembershipResult.from(membership),
            identity =
                CreatedChannelMemberIdentityResult(
                    userId = identity.userId,
                    email = identity.email,
                    nickname = identity.nickname,
                    temporaryPassword = identity.temporaryPassword,
                    created = identity.created,
                ),
        )
    }

    /**
     * Changes a channel membership role.
     */
    @Transactional
    fun changeRole(command: ChangeChannelMembershipRoleCommand): ChannelMembershipResult {
        val membership = loadOwnedMembership(command.channelId, command.membershipId)
        requireRoleChangeAllowed(command.actor, membership, command.role)
        protectLastActiveChannelAdmin(membership, nextRole = command.role, nextStatus = membership.status)
        return ChannelMembershipResult.from(channelMembershipRepository.save(membership.changeRole(command.role)))
    }

    /**
     * Enables a channel membership.
     */
    @Transactional
    fun enable(command: ChangeChannelMembershipStatusCommand): ChannelMembershipResult {
        val membership = loadOwnedMembership(command.channelId, command.membershipId)
        requireStatusChangeAllowed(command.actor, membership)
        return ChannelMembershipResult.from(channelMembershipRepository.save(membership.enable()))
    }

    /**
     * Disables a channel membership.
     */
    @Transactional
    fun disable(command: ChangeChannelMembershipStatusCommand): ChannelMembershipResult {
        val membership = loadOwnedMembership(command.channelId, command.membershipId)
        requireStatusChangeAllowed(command.actor, membership)
        protectLastActiveChannelAdmin(
            membership = membership,
            nextRole = membership.role,
            nextStatus = ChannelMembershipStatus.DISABLED,
        )
        return ChannelMembershipResult.from(channelMembershipRepository.save(membership.disable()))
    }

    /**
     * Verifies whether the actor can create the requested membership role.
     *
     * Policy details:
     * - PLATFORM_ADMIN can create both CHANNEL_ADMIN and AGENT memberships because platform operators own tenant setup.
     * - CHANNEL_ADMIN can create only AGENT memberships in their own channel because this delegates day-to-day staffing
     *   without allowing a tenant admin to mint another tenant admin.
     * - AGENT cannot manage memberships in Phase 6A. Agent self-service can be added later without changing this rule.
     */
    private fun requireCreatableRole(
        actor: AuthenticatedPrincipal,
        channelId: String,
        role: ChannelMembershipRole,
    ) {
        if (actor.isPlatformAdmin()) {
            return
        }
        channelAccessPolicy.requireChannelAdminWrite(actor, channelId)
        if (role != ChannelMembershipRole.AGENT) {
            throw ConversationException(
                error = ConversationError.CHANNEL_MEMBERSHIP_ROLE_NOT_ALLOWED,
                details = mapOf("channelId" to channelId, "role" to role.name),
            )
        }
    }

    /**
     * Verifies whether the actor can change the target membership role.
     *
     * Policy details:
     * - Role changes can elevate or remove tenant-level administrative authority, so Phase 6B keeps this operation
     *   PLATFORM_ADMIN-only. This avoids allowing a tenant admin to create another admin or demote a peer admin.
     * - CHANNEL_ADMIN can still create and enable/disable AGENT memberships for day-to-day staffing, but role mutation
     *   is intentionally more sensitive than lifecycle status changes.
     * - Even PLATFORM_ADMIN must leave at least one active CHANNEL_ADMIN in the channel. That invariant is checked by
     *   protectLastActiveChannelAdmin after this role authorization passes.
     */
    private fun requireRoleChangeAllowed(
        actor: AuthenticatedPrincipal,
        membership: ChannelMembership,
        nextRole: ChannelMembershipRole,
    ) {
        if (!actor.isPlatformAdmin()) {
            throw ConversationException(
                error = ConversationError.CHANNEL_MEMBERSHIP_ROLE_CHANGE_NOT_ALLOWED,
                details =
                    mapOf(
                        "channelId" to membership.channelId,
                        "membershipId" to membership.id,
                        "nextRole" to nextRole.name,
                    ),
            )
        }
    }

    /**
     * Verifies whether the actor can enable or disable the target membership.
     *
     * Policy details:
     * - PLATFORM_ADMIN can enable/disable every membership because platform operators are the recovery path for tenants.
     * - CHANNEL_ADMIN can enable/disable only AGENT memberships in their own channel. This delegates normal staffing
     *   operations while preventing tenant admins from locking out peer admins.
     * - CHANNEL_ADMIN cannot disable themselves. Self-disable is easy to do accidentally and can remove the actor's
     *   only management path before the UI can recover.
     * - AGENT cannot manage membership lifecycle in Phase 6B.
     */
    private fun requireStatusChangeAllowed(
        actor: AuthenticatedPrincipal,
        membership: ChannelMembership,
    ) {
        if (actor.isPlatformAdmin()) {
            return
        }
        val actorMembership =
            channelAccessPolicy.requireChannelAdminMembership(
                principal = actor,
                channelId = membership.channelId,
            )
        if (actorMembership.id == membership.id) {
            throw ConversationException(
                error = ConversationError.CHANNEL_MEMBERSHIP_SELF_CHANGE_NOT_ALLOWED,
                details = mapOf("membershipId" to membership.id),
            )
        }
        if (membership.role != ChannelMembershipRole.AGENT) {
            throw ConversationException(
                error = ConversationError.CHANNEL_MEMBERSHIP_STATUS_CHANGE_NOT_ALLOWED,
                details =
                    mapOf(
                        "channelId" to membership.channelId,
                        "membershipId" to membership.id,
                        "role" to membership.role.name,
                    ),
            )
        }
    }

    /**
     * Protects the invariant that every channel keeps at least one active CHANNEL_ADMIN.
     *
     * Policy details:
     * - This guard runs before role demotion and disable operations.
     * - The current membership is counted only when it is currently an active CHANNEL_ADMIN.
     * - If the requested next state would make that membership no longer active CHANNEL_ADMIN and it is the last one,
     *   the operation is rejected. This keeps tenant recovery possible without direct database intervention.
     */
    private fun protectLastActiveChannelAdmin(
        membership: ChannelMembership,
        nextRole: ChannelMembershipRole,
        nextStatus: ChannelMembershipStatus,
    ) {
        val currentlyActiveAdmin =
            membership.role == ChannelMembershipRole.CHANNEL_ADMIN &&
                membership.status == ChannelMembershipStatus.ACTIVE
        val remainsActiveAdmin =
            nextRole == ChannelMembershipRole.CHANNEL_ADMIN &&
                nextStatus == ChannelMembershipStatus.ACTIVE
        if (!currentlyActiveAdmin || remainsActiveAdmin) {
            return
        }
        val activeAdminCount =
            channelMembershipRepository.countByChannelIdAndRoleAndStatus(
                channelId = membership.channelId,
                role = ChannelMembershipRole.CHANNEL_ADMIN,
                status = ChannelMembershipStatus.ACTIVE,
            )
        if (activeAdminCount <= 1) {
            throw ConversationException(
                error = ConversationError.CHANNEL_MEMBERSHIP_LAST_ADMIN_REQUIRED,
                details = mapOf("channelId" to membership.channelId, "membershipId" to membership.id),
            )
        }
    }

    /**
     * Verifies that the target channel exists before identity or membership side effects happen.
     */
    private fun requireChannelExists(channelId: String) {
        if (channelRepository.findById(channelId) == null) {
            throw ConversationException(
                error = ConversationError.CHANNEL_NOT_FOUND,
                details = mapOf("channelId" to channelId),
            )
        }
    }

    /**
     * Loads a membership and verifies channel ownership.
     */
    private fun loadOwnedMembership(
        channelId: String,
        membershipId: String,
    ): ChannelMembership {
        val membership =
            channelMembershipRepository.findById(membershipId)
                ?: throw ConversationException(
                    error = ConversationError.CHANNEL_MEMBERSHIP_NOT_FOUND,
                    details = mapOf("membershipId" to membershipId),
                )
        if (membership.channelId != channelId) {
            throw ConversationException(
                error = ConversationError.CHANNEL_MEMBERSHIP_NOT_FOUND,
                details = mapOf("channelId" to channelId, "membershipId" to membershipId),
            )
        }
        return membership
    }
}

data class CreateChannelMembershipCommand(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val email: String,
    val nickname: String,
    val role: ChannelMembershipRole,
)

data class ChangeChannelMembershipRoleCommand(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val membershipId: String,
    val role: ChannelMembershipRole,
)

data class ChangeChannelMembershipStatusCommand(
    val actor: AuthenticatedPrincipal,
    val channelId: String,
    val membershipId: String,
)

data class ChannelMembershipCreationResult(
    val membership: ChannelMembershipResult,
    val identity: CreatedChannelMemberIdentityResult,
)

data class CreatedChannelMemberIdentityResult(
    val userId: String,
    val email: String,
    val nickname: String,
    val temporaryPassword: String?,
    val created: Boolean,
)

data class ChannelMembershipResult(
    val id: String,
    val channelId: String,
    val userId: String,
    val role: ChannelMembershipRole,
    val agentStatus: AgentStatus,
    val status: ChannelMembershipStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        /**
         * Maps a channel membership aggregate to an application result.
         */
        fun from(membership: ChannelMembership): ChannelMembershipResult =
            ChannelMembershipResult(
                id = membership.id,
                channelId = membership.channelId,
                userId = membership.userId,
                role = membership.role,
                agentStatus = membership.agentStatus,
                status = membership.status,
                createdAt = membership.createdAt,
                updatedAt = membership.updatedAt,
            )
    }
}
