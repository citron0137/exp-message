package site.rahoon.message.monolithic.core.iam.identity.infrastructure.bootstrap

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.core.iam.identity.application.facade.CreatePlatformAdminIfAbsentCommand
import site.rahoon.message.monolithic.core.iam.identity.application.facade.IdentityFacade

@Component
@Order(Int.MIN_VALUE + 1)
class CoreDefaultPlatformAdminStartupRunner(
    private val identityFacade: IdentityFacade,
    @Value("\${core.default-platform-admin.enabled:true}") private val enabled: Boolean,
    @Value("\${core.default-platform-admin.email:\${default-admin.email:admin@example.com}}") private val email: String,
    @Value("\${core.default-platform-admin.password:\${default-admin.password:}}") private val password: String,
    @Value("\${core.default-platform-admin.nickname:\${default-admin.nickname:admin}}") private val nickname: String,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(CoreDefaultPlatformAdminStartupRunner::class.java)

    /**
     * Creates the first core platform admin during application startup.
     */
    override fun run(args: ApplicationArguments) {
        if (!enabled) {
            logger.info("Core default platform admin creation is disabled")
            return
        }
        val result =
            identityFacade.createPlatformAdminIfAbsent(
                CreatePlatformAdminIfAbsentCommand(
                    email = email,
                    password = password,
                    nickname = nickname,
                ),
            )
        if (result.created) {
            val passwordLog = result.temporaryPassword?.let { " temporaryPassword=$it" } ?: " password=(configured)"
            logger.info("Core default platform admin created: email=$email,$passwordLog")
        }
    }
}
