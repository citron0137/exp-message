package site.rahoon.message.monolithic.core.conversation.application.query

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationType

@Service
class WidgetBootstrapQueryService(
    private val widgetAccessPolicy: WidgetAccessPolicy,
) {
    /**
     * Resolves public widget bootstrap data from a public key and request origin.
     */
    @Transactional(readOnly = true)
    fun bootstrap(query: WidgetBootstrapQuery): WidgetBootstrapResult {
        val access = widgetAccessPolicy.requireAccessibleWidget(query.publicKey, query.origin)
        return WidgetBootstrapResult(
            channel =
                WidgetBootstrapChannelResult(
                    id = access.channel.id,
                    name = access.channel.name,
                ),
            integration =
                WidgetBootstrapIntegrationResult(
                    id = access.integration.id,
                    type = access.integration.type,
                    publicKey = access.integration.publicKey,
                ),
        )
    }
}

data class WidgetBootstrapQuery(
    val publicKey: String,
    val origin: String,
)

data class WidgetBootstrapResult(
    val channel: WidgetBootstrapChannelResult,
    val integration: WidgetBootstrapIntegrationResult,
)

data class WidgetBootstrapChannelResult(
    val id: String,
    val name: String,
)

data class WidgetBootstrapIntegrationResult(
    val id: String,
    val type: ChannelIntegrationType,
    val publicKey: String,
)
