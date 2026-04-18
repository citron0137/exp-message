package site.rahoon.message.monolithic.presentation.http.admin

import jakarta.validation.Valid
import jakarta.validation.constraints.NotNull
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.core.conversation.application.facade.AdminChannelIntegrationFacade
import site.rahoon.message.monolithic.core.conversation.application.facade.ChangeChannelIntegrationStatusCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.ChannelIntegrationResult
import site.rahoon.message.monolithic.core.conversation.application.facade.CreateWidgetIntegrationCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.UpdateChannelIntegrationAllowedOriginsCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.WidgetIntegrationCreationResult
import site.rahoon.message.monolithic.core.conversation.application.query.AdminChannelIntegrationQueryService
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.presentation.http.shared.ApiResponse
import java.time.LocalDateTime

@RestController
@RequestMapping("/admin/channels/{channelId}/integrations")
class CoreAdminChannelIntegrationController(
    private val adminChannelIntegrationFacade: AdminChannelIntegrationFacade,
    private val adminChannelIntegrationQueryService: AdminChannelIntegrationQueryService,
) {
    /**
     * Creates a widget integration for a channel.
     */
    @PostMapping("/widget")
    @ResponseStatus(HttpStatus.CREATED)
    fun createWidget(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @Valid @RequestBody request: AdminChannelIntegrationRequest.CreateWidget,
    ): ApiResponse<AdminChannelIntegrationResponse.Create> {
        val result =
            adminChannelIntegrationFacade.createWidgetIntegration(
                CreateWidgetIntegrationCommand(
                    actor = principal,
                    channelId = channelId,
                    allowedOrigins = request.allowedOrigins,
                ),
            )
        return ApiResponse.success(AdminChannelIntegrationResponse.Create.from(result))
    }

    /**
     * Lists integrations for a channel.
     */
    @GetMapping
    fun list(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
    ): ApiResponse<AdminChannelIntegrationResponse.ListResult> =
        ApiResponse.success(
            AdminChannelIntegrationResponse.ListResult.from(
                adminChannelIntegrationQueryService.listByChannel(principal, channelId),
            ),
        )

    /**
     * Enables a channel integration.
     */
    @PatchMapping("/{integrationId}/enable")
    fun enable(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @PathVariable integrationId: String,
    ): ApiResponse<AdminChannelIntegrationResponse.Detail> =
        ApiResponse.success(
            AdminChannelIntegrationResponse.Detail.from(
                adminChannelIntegrationFacade.enableIntegration(
                    ChangeChannelIntegrationStatusCommand(
                        actor = principal,
                        channelId = channelId,
                        integrationId = integrationId,
                    ),
                ),
            ),
        )

    /**
     * Disables a channel integration.
     */
    @PatchMapping("/{integrationId}/disable")
    fun disable(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @PathVariable integrationId: String,
    ): ApiResponse<AdminChannelIntegrationResponse.Detail> =
        ApiResponse.success(
            AdminChannelIntegrationResponse.Detail.from(
                adminChannelIntegrationFacade.disableIntegration(
                    ChangeChannelIntegrationStatusCommand(
                        actor = principal,
                        channelId = channelId,
                        integrationId = integrationId,
                    ),
                ),
            ),
        )

    /**
     * Replaces allowed origins for a channel integration.
     */
    @PatchMapping("/{integrationId}/allowed-origins")
    fun updateAllowedOrigins(
        principal: AuthenticatedPrincipal,
        @PathVariable channelId: String,
        @PathVariable integrationId: String,
        @Valid @RequestBody request: AdminChannelIntegrationRequest.UpdateAllowedOrigins,
    ): ApiResponse<AdminChannelIntegrationResponse.Detail> =
        ApiResponse.success(
            AdminChannelIntegrationResponse.Detail.from(
                adminChannelIntegrationFacade.updateAllowedOrigins(
                    UpdateChannelIntegrationAllowedOriginsCommand(
                        actor = principal,
                        channelId = channelId,
                        integrationId = integrationId,
                        allowedOrigins = request.allowedOrigins,
                    ),
                ),
            ),
        )
}

object AdminChannelIntegrationRequest {
    data class CreateWidget(
        @field:NotNull
        val allowedOrigins: List<String> = emptyList(),
    )

    data class UpdateAllowedOrigins(
        @field:NotNull
        val allowedOrigins: List<String> = emptyList(),
    )
}

object AdminChannelIntegrationResponse {
    data class ListResult(
        val items: List<Detail>,
    ) {
        companion object {
            /**
             * Maps integration results to a list response.
             */
            fun from(results: List<ChannelIntegrationResult>): ListResult = ListResult(items = results.map { Detail.from(it) })
        }
    }

    data class Create(
        val integration: Detail,
        val secret: String,
    ) {
        companion object {
            /**
             * Maps a widget integration creation result to a response.
             */
            fun from(result: WidgetIntegrationCreationResult): Create =
                Create(
                    integration = Detail.from(result.integration),
                    secret = result.secret,
                )
        }
    }

    data class Detail(
        val id: String,
        val channelId: String,
        val type: String,
        val publicKey: String,
        val status: String,
        val allowedOrigins: List<String>,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            /**
             * Maps a channel integration result to a response.
             */
            fun from(result: ChannelIntegrationResult): Detail =
                Detail(
                    id = result.id,
                    channelId = result.channelId,
                    type = result.type.name,
                    publicKey = result.publicKey,
                    status = result.status.name,
                    allowedOrigins = result.allowedOrigins,
                    createdAt = result.createdAt,
                    updatedAt = result.updatedAt,
                )
        }
    }
}
