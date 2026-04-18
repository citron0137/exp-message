package site.rahoon.message.monolithic.presentation.http.widget

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.core.conversation.application.query.WidgetBootstrapQuery
import site.rahoon.message.monolithic.core.conversation.application.query.WidgetBootstrapQueryService
import site.rahoon.message.monolithic.core.conversation.application.query.WidgetBootstrapResult
import site.rahoon.message.monolithic.presentation.http.shared.ApiResponse

@RestController
@RequestMapping("/widget/bootstrap")
class WidgetBootstrapController(
    private val widgetBootstrapQueryService: WidgetBootstrapQueryService,
) {
    /**
     * Resolves widget bootstrap data from a public key and request origin.
     */
    @PostMapping
    fun bootstrap(
        servletRequest: HttpServletRequest,
        @Valid @RequestBody request: WidgetBootstrapRequest.Bootstrap,
    ): ApiResponse<WidgetBootstrapResponse.Bootstrap> {
        val origin = servletRequest.getHeader("Origin") ?: request.origin
        val result =
            widgetBootstrapQueryService.bootstrap(
                WidgetBootstrapQuery(
                    publicKey = request.publicKey,
                    origin = origin.orEmpty(),
                ),
            )
        return ApiResponse.success(WidgetBootstrapResponse.Bootstrap.from(result))
    }
}

object WidgetBootstrapRequest {
    data class Bootstrap(
        @field:NotBlank
        val publicKey: String,
        val origin: String? = null,
    )
}

object WidgetBootstrapResponse {
    data class Bootstrap(
        val channel: Channel,
        val integration: Integration,
    ) {
        companion object {
            /**
             * Maps a widget bootstrap result to a response.
             */
            fun from(result: WidgetBootstrapResult): Bootstrap =
                Bootstrap(
                    channel =
                        Channel(
                            id = result.channel.id,
                            name = result.channel.name,
                        ),
                    integration =
                        Integration(
                            id = result.integration.id,
                            type = result.integration.type.name,
                            publicKey = result.integration.publicKey,
                        ),
                )
        }
    }

    data class Channel(
        val id: String,
        val name: String,
    )

    data class Integration(
        val id: String,
        val type: String,
        val publicKey: String,
    )
}
