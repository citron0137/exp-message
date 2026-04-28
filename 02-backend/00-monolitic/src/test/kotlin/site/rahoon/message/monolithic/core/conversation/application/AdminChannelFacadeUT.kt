package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.facade.AdminChannelFacade
import site.rahoon.message.monolithic.core.conversation.application.facade.CreateAdminChannelCommand
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.application.port.CustomerAdminIdentity
import site.rahoon.message.monolithic.core.conversation.application.port.CustomerAdminIdentityPort
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelMembershipRole
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import java.time.LocalDateTime

class AdminChannelFacadeUT {
    private lateinit var channelRepository: ChannelRepository
    private lateinit var channelMembershipRepository: ChannelMembershipRepository
    private lateinit var customerAdminIdentityPort: CustomerAdminIdentityPort
    private lateinit var facade: AdminChannelFacade

    @BeforeEach
    fun setUp() {
        channelRepository = mockk()
        channelMembershipRepository = mockk()
        customerAdminIdentityPort = mockk()
        facade =
            AdminChannelFacade(
                channelRepository = channelRepository,
                channelMembershipRepository = channelMembershipRepository,
                customerAdminIdentityPort = customerAdminIdentityPort,
                channelAccessPolicy = ChannelAccessPolicy(channelMembershipRepository),
            )
    }

    @Test
    fun `createChannel creates channel and initial channel admin membership`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelRepository.save(any()) } answers { firstArg() }
        every { customerAdminIdentityPort.createOrLoadCustomerAdmin(any()) } returns
            CustomerAdminIdentity(
                userId = "customer-admin-1",
                email = "customer@example.com",
                nickname = "Customer",
                temporaryPassword = "temporary-password",
                created = true,
            )
        every { channelMembershipRepository.save(any()) } answers { firstArg() }

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result =
            facade.createChannel(
                CreateAdminChannelCommand(
                    actor = actor,
                    name = "Acme",
                    adminEmail = "customer@example.com",
                    adminNickname = "Customer",
                ),
            )

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result.channel.name shouldBe "Acme"
        result.initialAdmin.userId shouldBe "customer-admin-1"
        result.initialAdmin.email shouldBe "customer@example.com"
        result.initialAdmin.temporaryPassword shouldBe "temporary-password"
        result.initialAdmin.created shouldBe true
        verify {
            customerAdminIdentityPort.createOrLoadCustomerAdmin(
                match {
                    it.email == "customer@example.com" &&
                        it.nickname == "Customer"
                },
            )
        }
        verify {
            channelMembershipRepository.save(
                match {
                    it.channelId == result.channel.id &&
                        it.userId == "customer-admin-1" &&
                        it.role == ChannelMembershipRole.CHANNEL_ADMIN
                },
            )
        }
    }

    @Test
    fun `createChannel rejects non platform admin actor`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.createChannel(
                    CreateAdminChannelCommand(
                        actor = actor,
                        name = "Acme",
                        adminEmail = "customer@example.com",
                        adminNickname = "Customer",
                    ),
                )
            }

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        exception.error shouldBe ConversationError.PLATFORM_ADMIN_REQUIRED
        verify(exactly = 0) { channelRepository.save(any()) }
        verify(exactly = 0) { customerAdminIdentityPort.createOrLoadCustomerAdmin(any()) }
        verify(exactly = 0) { channelMembershipRepository.save(any()) }
    }

    @Test
    fun `listChannels returns mapped channels for platform admin`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        val channels = listOf(Channel.create("Acme"), Channel.create("Beta"))
        every { channelRepository.findAll() } returns channels

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result = facade.listChannels(actor)

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result.shouldHaveSize(2)
        result[0].id shouldBe channels[0].id
        result[0].name shouldBe "Acme"
        result[1].id shouldBe channels[1].id
        result[1].name shouldBe "Beta"
        verify { channelRepository.findAll() }
    }

    @Test
    fun `getChannel throws when channel does not exist`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelRepository.findById("missing-channel") } returns null

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.getChannel(
                    actor = actor,
                    channelId = "missing-channel",
                )
            }

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_NOT_FOUND
        exception.details["channelId"] shouldBe "missing-channel"
    }

    private fun principal(globalRole: PrincipalGlobalRole): AuthenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = "user-1",
            sessionId = "session-1",
            globalRole = globalRole,
            expiresAt = LocalDateTime.now().plusHours(1),
        )
}
