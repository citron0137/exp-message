package site.rahoon.message.monolithic.presentation.http.admin

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.core.conversation.application.facade.AdminChannelMembershipFacade
import site.rahoon.message.monolithic.core.conversation.application.facade.ChangeChannelMembershipRoleCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.ChangeChannelMembershipStatusCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.ChannelMembershipCreationResult
import site.rahoon.message.monolithic.core.conversation.application.facade.ChannelMembershipResult
import site.rahoon.message.monolithic.core.conversation.application.facade.CreateChannelMembershipCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.CreatedChannelMemberIdentityResult
import site.rahoon.message.monolithic.core.conversation.application.query.AdminChannelMembershipQueryService
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipStatus
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.presentation.http.shared.ApiResponse
import java.time.LocalDateTime

@RestController
@RequestMapping("/admin/channels/{channelId}/memberships")
class AdminChannelMembershipController(
    private val adminChannelMembershipFacade: AdminChannelMembershipFacade,
    private val adminChannelMembershipQueryService: AdminChannelMembershipQueryService,
) {
    /**
     * Creates a channel membership and its backing identity if needed.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @Valid @RequestBody request: AdminChannelMembershipRequest.Create,
    ): ApiResponse<AdminChannelMembershipResponse.Create> =
        ApiResponse.success(
            AdminChannelMembershipResponse.Create.from(
                adminChannelMembershipFacade.createMembership(
                    CreateChannelMembershipCommand(
                        actor = principal,
                        channelId = channelId,
                        email = request.email,
                        nickname = request.nickname,
                        role = request.role,
                    ),
                ),
            ),
        )

    /**
     * Lists channel memberships.
     */
    @GetMapping
    fun list(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @RequestParam(required = false) role: ChannelMembershipRole?,
        @RequestParam(required = false) status: ChannelMembershipStatus?,
    ): ApiResponse<AdminChannelMembershipResponse.ListResult> =
        ApiResponse.success(
            AdminChannelMembershipResponse.ListResult.from(
                adminChannelMembershipQueryService.listByChannel(
                    actor = principal,
                    channelId = channelId,
                    role = role,
                    status = status,
                ),
            ),
        )

    /**
     * Changes a channel membership role.
     */
    @PatchMapping("/{membershipId}/role")
    fun changeRole(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @PathVariable membershipId: String,
        @Valid @RequestBody request: AdminChannelMembershipRequest.ChangeRole,
    ): ApiResponse<AdminChannelMembershipResponse.Detail> =
        ApiResponse.success(
            AdminChannelMembershipResponse.Detail.from(
                adminChannelMembershipFacade.changeRole(
                    ChangeChannelMembershipRoleCommand(
                        actor = principal,
                        channelId = channelId,
                        membershipId = membershipId,
                        role = request.role,
                    ),
                ),
            ),
        )

    /**
     * Enables a channel membership.
     */
    @PatchMapping("/{membershipId}/enable")
    fun enable(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @PathVariable membershipId: String,
    ): ApiResponse<AdminChannelMembershipResponse.Detail> =
        ApiResponse.success(
            AdminChannelMembershipResponse.Detail.from(
                adminChannelMembershipFacade.enable(
                    ChangeChannelMembershipStatusCommand(
                        actor = principal,
                        channelId = channelId,
                        membershipId = membershipId,
                    ),
                ),
            ),
        )

    /**
     * Disables a channel membership.
     */
    @PatchMapping("/{membershipId}/disable")
    fun disable(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @PathVariable membershipId: String,
    ): ApiResponse<AdminChannelMembershipResponse.Detail> =
        ApiResponse.success(
            AdminChannelMembershipResponse.Detail.from(
                adminChannelMembershipFacade.disable(
                    ChangeChannelMembershipStatusCommand(
                        actor = principal,
                        channelId = channelId,
                        membershipId = membershipId,
                    ),
                ),
            ),
        )
}

object AdminChannelMembershipRequest {
    data class Create(
        @field:Email
        @field:NotBlank
        val email: String,
        @field:NotBlank
        val nickname: String,
        @field:NotNull
        val role: ChannelMembershipRole,
    )

    data class ChangeRole(
        @field:NotNull
        val role: ChannelMembershipRole,
    )
}

object AdminChannelMembershipResponse {
    data class ListResult(
        val items: List<Detail>,
    ) {
        companion object {
            /**
             * Maps membership results to a list response.
             */
            fun from(results: List<ChannelMembershipResult>): ListResult = ListResult(items = results.map { Detail.from(it) })
        }
    }

    data class Create(
        val membership: Detail,
        val identity: Identity,
    ) {
        companion object {
            /**
             * Maps a membership creation result to a response.
             */
            fun from(result: ChannelMembershipCreationResult): Create =
                Create(
                    membership = Detail.from(result.membership),
                    identity = Identity.from(result.identity),
                )
        }
    }

    data class Detail(
        val id: String,
        val channelId: String,
        val userId: String,
        val role: String,
        val agentStatus: String,
        val status: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            /**
             * Maps a channel membership result to a response.
             */
            fun from(result: ChannelMembershipResult): Detail =
                Detail(
                    id = result.id,
                    channelId = result.channelId,
                    userId = result.userId,
                    role = result.role.name,
                    agentStatus = result.agentStatus.name,
                    status = result.status.name,
                    createdAt = result.createdAt,
                    updatedAt = result.updatedAt,
                )
        }
    }

    data class Identity(
        val userId: String,
        val email: String,
        val nickname: String,
        val temporaryPassword: String?,
        val created: Boolean,
    ) {
        companion object {
            /**
             * Maps a channel member identity result to a response.
             */
            fun from(result: CreatedChannelMemberIdentityResult): Identity =
                Identity(
                    userId = result.userId,
                    email = result.email,
                    nickname = result.nickname,
                    temporaryPassword = result.temporaryPassword,
                    created = result.created,
                )
        }
    }
}
