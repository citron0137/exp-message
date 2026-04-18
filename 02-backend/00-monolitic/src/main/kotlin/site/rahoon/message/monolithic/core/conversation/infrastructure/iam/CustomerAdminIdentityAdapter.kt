package site.rahoon.message.monolithic.core.conversation.infrastructure.iam

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.conversation.application.port.CreateOrLoadCustomerAdminIdentityCommand
import site.rahoon.message.monolithic.core.conversation.application.port.CustomerAdminIdentity
import site.rahoon.message.monolithic.core.conversation.application.port.CustomerAdminIdentityPort
import site.rahoon.message.monolithic.core.iam.identity.application.facade.CreateOrLoadCustomerAdminCommand
import site.rahoon.message.monolithic.core.iam.identity.application.facade.IdentityFacade

@Component
class CustomerAdminIdentityAdapter(
    private val identityFacade: IdentityFacade,
) : CustomerAdminIdentityPort {
    /**
     * Creates or loads a customer admin identity through the identity facade.
     */
    override fun createOrLoadCustomerAdmin(command: CreateOrLoadCustomerAdminIdentityCommand): CustomerAdminIdentity {
        val result =
            identityFacade.createOrLoadCustomerAdmin(
                CreateOrLoadCustomerAdminCommand(
                    email = command.email,
                    nickname = command.nickname,
                ),
            )
        return CustomerAdminIdentity(
            userId = result.userId,
            email = result.email,
            nickname = result.nickname,
            temporaryPassword = result.temporaryPassword,
            created = result.created,
        )
    }
}
