package site.rahoon.message.monolithic.core.conversation.application.port

interface CustomerAdminIdentityPort {
    /**
     * Creates or loads a customer admin identity.
     */
    fun createOrLoadCustomerAdmin(command: CreateOrLoadCustomerAdminIdentityCommand): CustomerAdminIdentity
}

data class CreateOrLoadCustomerAdminIdentityCommand(
    val email: String,
    val nickname: String,
)

data class CustomerAdminIdentity(
    val userId: String,
    val email: String,
    val nickname: String,
    val temporaryPassword: String?,
    val created: Boolean,
)
