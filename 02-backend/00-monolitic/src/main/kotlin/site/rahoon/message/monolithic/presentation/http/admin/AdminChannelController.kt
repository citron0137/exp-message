package site.rahoon.message.monolithic.presentation.http.admin

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.core.conversation.application.facade.AdminChannelCreationResult
import site.rahoon.message.monolithic.core.conversation.application.facade.AdminChannelFacade
import site.rahoon.message.monolithic.core.conversation.application.facade.ChannelResult
import site.rahoon.message.monolithic.core.conversation.application.facade.CreateAdminChannelCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.InitialChannelAdminResult
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.presentation.http.shared.ApiResponse
import java.time.LocalDateTime

@RestController
@RequestMapping("/admin/channels")
class CoreAdminChannelController(
    private val adminChannelFacade: AdminChannelFacade,
) {
    /**
     * Creates a channel and its initial customer admin.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(
        principal: AuthenticatedPrincipal,
        @Valid @RequestBody request: AdminChannelRequest.Create,
    ): ApiResponse<AdminChannelResponse.Create> {
        val result =
            adminChannelFacade.createChannel(
                CreateAdminChannelCommand(
                    actor = principal,
                    name = request.name,
                    adminEmail = request.adminEmail,
                    adminNickname = request.adminNickname,
                ),
            )
        return ApiResponse.success(AdminChannelResponse.Create.from(result))
    }

    /**
     * Lists all channels for a platform admin.
     */
    @GetMapping
    fun list(principal: AuthenticatedPrincipal): ApiResponse<AdminChannelResponse.ListResult> =
        ApiResponse.success(AdminChannelResponse.ListResult.from(adminChannelFacade.listChannels(principal)))

    /**
     * Gets a channel by identifier for a platform admin.
     */
    @GetMapping("/{id}")
    fun getById(
        principal: AuthenticatedPrincipal,
        @PathVariable id: String,
    ): ApiResponse<AdminChannelResponse.Detail> =
        ApiResponse.success(AdminChannelResponse.Detail.from(adminChannelFacade.getChannel(principal, id)))
}

object AdminChannelRequest {
    data class Create(
        @field:NotBlank
        val name: String,
        @field:Email
        @field:NotBlank
        val adminEmail: String,
        @field:NotBlank
        val adminNickname: String,
    )
}

object AdminChannelResponse {
    data class ListResult(
        val items: List<Detail>,
    ) {
        companion object {
            /**
             * Maps channel results to a list response.
             */
            fun from(results: List<ChannelResult>): ListResult = ListResult(items = results.map { Detail.from(it) })
        }
    }

    data class Create(
        val channel: Detail,
        val initialAdmin: InitialAdmin,
    ) {
        companion object {
            /**
             * Maps a channel creation result to a response.
             */
            fun from(result: AdminChannelCreationResult): Create =
                Create(
                    channel = Detail.from(result.channel),
                    initialAdmin = InitialAdmin.from(result.initialAdmin),
                )
        }
    }

    data class Detail(
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
            fun from(result: ChannelResult): Detail =
                Detail(
                    id = result.id,
                    name = result.name,
                    status = result.status.name,
                    createdAt = result.createdAt,
                    updatedAt = result.updatedAt,
                )
        }
    }

    data class InitialAdmin(
        val userId: String,
        val email: String,
        val nickname: String,
        val temporaryPassword: String?,
        val created: Boolean,
    ) {
        companion object {
            /**
             * Maps an initial admin result to a response.
             */
            fun from(result: InitialChannelAdminResult): InitialAdmin =
                InitialAdmin(
                    userId = result.userId,
                    email = result.email,
                    nickname = result.nickname,
                    temporaryPassword = result.temporaryPassword,
                    created = result.created,
                )
        }
    }
}
