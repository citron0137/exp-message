package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.facade.AdminChannelIntegrationFacade
import site.rahoon.message.monolithic.core.conversation.application.facade.ChangeChannelIntegrationStatusCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.CreateWidgetIntegrationCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.UpdateChannelIntegrationAllowedOriginsCommand
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelIntegrationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.application.port.IntegrationKeyGenerator
import site.rahoon.message.monolithic.core.conversation.application.port.IntegrationSecretHasher
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AllowedOrigins
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegrationType
import site.rahoon.message.monolithic.core.conversation.domain.ChannelStatus
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import java.time.LocalDateTime

class AdminChannelIntegrationFacadeUT {
    private lateinit var channelRepository: ChannelRepository
    private lateinit var channelIntegrationRepository: ChannelIntegrationRepository
    private lateinit var integrationKeyGenerator: IntegrationKeyGenerator
    private lateinit var integrationSecretHasher: IntegrationSecretHasher
    private lateinit var facade: AdminChannelIntegrationFacade

    @BeforeEach
    fun setUp() {
        channelRepository = mockk()
        channelIntegrationRepository = mockk()
        integrationKeyGenerator = mockk()
        integrationSecretHasher = mockk()
        facade =
            AdminChannelIntegrationFacade(
                channelRepository = channelRepository,
                channelIntegrationRepository = channelIntegrationRepository,
                integrationKeyGenerator = integrationKeyGenerator,
                integrationSecretHasher = integrationSecretHasher,
                channelAccessPolicy = ChannelAccessPolicy(),
            )
    }

    @Test
    fun `createWidgetIntegration creates active widget integration and returns one-time secret`() {
        // Arrange: Prepare an active channel and generated integration credentials. / 준비: 활성 채널과 생성된 integration credential을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelRepository.findById("channel-1") } returns channel("channel-1", ChannelStatus.ACTIVE)
        every {
            channelIntegrationRepository.existsByChannelIdAndTypeAndStatus(
                "channel-1",
                ChannelIntegrationType.WIDGET,
                ChannelIntegrationStatus.ACTIVE,
            )
        } returns false
        every { integrationKeyGenerator.generateWidgetPublicKey() } returns "wpk_public"
        every { integrationKeyGenerator.generateWidgetSecret() } returns "wsk_secret"
        every { integrationSecretHasher.hash("wsk_secret") } returns "hashed-secret"
        every { channelIntegrationRepository.save(any()) } answers { firstArg() }

        // Act: Create a widget integration through the facade. / 실행: facade를 통해 widget integration을 생성한다.
        val result =
            facade.createWidgetIntegration(
                CreateWidgetIntegrationCommand(
                    actor = actor,
                    channelId = "channel-1",
                    allowedOrigins = listOf(" https://acme.com ", "*"),
                ),
            )

        // Assert: Verify the integration, secret, and saved aggregate. / 검증: integration, secret, 저장된 aggregate를 검증한다.
        result.integration.channelId shouldBe "channel-1"
        result.integration.type shouldBe ChannelIntegrationType.WIDGET
        result.integration.publicKey shouldBe "wpk_public"
        result.integration.status shouldBe ChannelIntegrationStatus.ACTIVE
        result.integration.allowedOrigins shouldBe listOf("https://acme.com", "*")
        result.secret shouldBe "wsk_secret"
        verify {
            channelIntegrationRepository.save(
                match {
                    it.publicKey == "wpk_public" &&
                        it.secretHash == "hashed-secret" &&
                        it.allowedOrigins.values == listOf("https://acme.com", "*")
                },
            )
        }
    }

    @Test
    fun `createWidgetIntegration throws when active widget integration already exists`() {
        // Arrange: Prepare an active channel that already has an active widget integration. / 준비: 이미 활성 widget integration이 있는 활성 채널을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelRepository.findById("channel-1") } returns channel("channel-1", ChannelStatus.ACTIVE)
        every {
            channelIntegrationRepository.existsByChannelIdAndTypeAndStatus(
                "channel-1",
                ChannelIntegrationType.WIDGET,
                ChannelIntegrationStatus.ACTIVE,
            )
        } returns true

        // Act: Try to create another active widget integration. / 실행: 다른 활성 widget integration 생성을 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.createWidgetIntegration(
                    CreateWidgetIntegrationCommand(
                        actor = actor,
                        channelId = "channel-1",
                        allowedOrigins = listOf("*"),
                    ),
                )
            }

        // Assert: Verify duplicate active widget integration is rejected. / 검증: 중복 활성 widget integration이 거부되는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_INTEGRATION_ALREADY_EXISTS
        verify(exactly = 0) { channelIntegrationRepository.save(any()) }
    }

    @Test
    fun `createWidgetIntegration throws when channel is inactive`() {
        // Arrange: Prepare an inactive channel. / 준비: 비활성 채널을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        every { channelRepository.findById("channel-1") } returns channel("channel-1", ChannelStatus.INACTIVE)

        // Act: Try to create a widget integration for the inactive channel. / 실행: 비활성 채널에 widget integration 생성을 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.createWidgetIntegration(
                    CreateWidgetIntegrationCommand(
                        actor = actor,
                        channelId = "channel-1",
                        allowedOrigins = listOf("*"),
                    ),
                )
            }

        // Assert: Verify inactive channels cannot receive active integrations. / 검증: 비활성 채널에는 활성 integration을 만들 수 없는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_NOT_ACTIVE
        verify(exactly = 0) { channelIntegrationRepository.save(any()) }
    }

    @Test
    fun `enableIntegration enables disabled integration when no other active widget exists`() {
        // Arrange: Prepare a disabled integration and no active sibling. / 준비: 비활성 integration과 활성 sibling이 없는 상태를 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        val integration = widgetIntegration().disable()
        every { channelRepository.findById("channel-1") } returns channel("channel-1", ChannelStatus.ACTIVE)
        every { channelIntegrationRepository.findById(integration.id) } returns integration
        every {
            channelIntegrationRepository.existsByChannelIdAndTypeAndStatusAndIdNot(
                "channel-1",
                ChannelIntegrationType.WIDGET,
                ChannelIntegrationStatus.ACTIVE,
                integration.id,
            )
        } returns false
        every { channelIntegrationRepository.save(any()) } answers { firstArg() }

        // Act: Enable the integration. / 실행: integration을 활성화한다.
        val result =
            facade.enableIntegration(
                ChangeChannelIntegrationStatusCommand(
                    actor = actor,
                    channelId = "channel-1",
                    integrationId = integration.id,
                ),
            )

        // Assert: Verify the integration is active. / 검증: integration이 활성 상태인지 검증한다.
        result.status shouldBe ChannelIntegrationStatus.ACTIVE
    }

    @Test
    fun `disableIntegration disables owned integration`() {
        // Arrange: Prepare an active owned integration. / 준비: 소유한 활성 integration을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        val integration = widgetIntegration()
        every { channelIntegrationRepository.findById(integration.id) } returns integration
        every { channelIntegrationRepository.save(any()) } answers { firstArg() }

        // Act: Disable the integration. / 실행: integration을 비활성화한다.
        val result =
            facade.disableIntegration(
                ChangeChannelIntegrationStatusCommand(
                    actor = actor,
                    channelId = "channel-1",
                    integrationId = integration.id,
                ),
            )

        // Assert: Verify the integration is disabled. / 검증: integration이 비활성 상태인지 검증한다.
        result.status shouldBe ChannelIntegrationStatus.DISABLED
    }

    @Test
    fun `updateAllowedOrigins replaces origins on owned integration`() {
        // Arrange: Prepare an integration with old origins. / 준비: 기존 origins를 가진 integration을 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        val integration = widgetIntegration()
        every { channelIntegrationRepository.findById(integration.id) } returns integration
        every { channelIntegrationRepository.save(any()) } answers { firstArg() }

        // Act: Replace allowed origins. / 실행: allowed origins를 교체한다.
        val result =
            facade.updateAllowedOrigins(
                UpdateChannelIntegrationAllowedOriginsCommand(
                    actor = actor,
                    channelId = "channel-1",
                    integrationId = integration.id,
                    allowedOrigins = listOf("*", "https://beta.com"),
                ),
            )

        // Assert: Verify the result contains replacement origins. / 검증: 결과에 교체된 origins가 포함되는지 검증한다.
        result.allowedOrigins shouldBe listOf("*", "https://beta.com")
    }

    private fun principal(globalRole: PrincipalGlobalRole): AuthenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = "user-1",
            sessionId = "session-1",
            globalRole = globalRole,
            expiresAt = LocalDateTime.now().plusHours(1),
        )

    private fun channel(
        id: String,
        status: ChannelStatus,
    ): Channel =
        Channel(
            id = id,
            name = "Acme",
            status = status,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun widgetIntegration(): ChannelIntegration =
        ChannelIntegration.createWidget(
            channelId = "channel-1",
            publicKey = "wpk_public",
            secretHash = "hashed-secret",
            allowedOrigins = AllowedOrigins.of(listOf("https://acme.com")),
        )
}
