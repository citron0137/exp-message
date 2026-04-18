package site.rahoon.message.monolithic.presentation.http.admin

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.core.conversation.application.facade.ChannelMembershipResult
import site.rahoon.message.monolithic.core.conversation.application.facade.ChannelResult
import site.rahoon.message.monolithic.core.conversation.application.query.AdminWorkspaceChannelResult
import site.rahoon.message.monolithic.core.conversation.application.query.AdminWorkspaceQueryService
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.presentation.http.shared.ApiResponse
import java.time.LocalDateTime

@RestController
@RequestMapping("/admin/me")
class AdminWorkspaceController(
    private val adminWorkspaceQueryService: AdminWorkspaceQueryService,
) {
    /**
     * Lists channels available to the current admin user.
     */
    @GetMapping("/channels")
    fun listMyChannels(principal: AuthenticatedPrincipal): ApiResponse<AdminWorkspaceResponse.ChannelList> =
        ApiResponse.success(AdminWorkspaceResponse.ChannelList.from(adminWorkspaceQueryService.listMyChannels(principal)))
}

object AdminWorkspaceResponse {
    data class ChannelList(
        val items: List<Item>,
    ) {
        companion object {
            /**
             * Maps workspace channel results to a response.
             */
            fun from(results: List<AdminWorkspaceChannelResult>): ChannelList = ChannelList(items = results.map { Item.from(it) })
        }
    }

    data class Item(
        val channel: Channel,
        val membership: Membership?,
    ) {
        companion object {
            /**
             * Maps one workspace channel result to a response item.
             */
            fun from(result: AdminWorkspaceChannelResult): Item =
                Item(
                    channel = Channel.from(result.channel),
                    membership = result.membership?.let { Membership.from(it) },
                )
        }
    }

    data class Channel(
        val id: String,
        val name: String,
        val status: String,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            /**
             * Maps a channel result to a response.
             */
            fun from(result: ChannelResult): Channel =
                Channel(
                    id = result.id,
                    name = result.name,
                    status = result.status.name,
                    createdAt = result.createdAt,
                    updatedAt = result.updatedAt,
                )
        }
    }

    data class Membership(
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
             * Maps a membership result to a response.
             */
            fun from(result: ChannelMembershipResult): Membership =
                Membership(
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
}
