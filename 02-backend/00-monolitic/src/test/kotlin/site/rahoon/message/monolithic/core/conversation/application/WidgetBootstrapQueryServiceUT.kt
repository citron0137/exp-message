package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelIntegrationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelRepository
import site.rahoon.message.monolithic.core.conversation.application.query.WidgetBootstrapQuery
import site.rahoon.message.monolithic.core.conversation.application.query.WidgetBootstrapQueryService
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AllowedOrigins
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.domain.ChannelStatus
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import java.time.LocalDateTime

class WidgetBootstrapQueryServiceUT {
    private lateinit var channelIntegrationRepository: ChannelIntegrationRepository
    private lateinit var channelRepository: ChannelRepository
    private lateinit var queryService: WidgetBootstrapQueryService

    @BeforeEach
    fun setUp() {
        channelIntegrationRepository = mockk()
        channelRepository = mockk()
        queryService =
            WidgetBootstrapQueryService(
                widgetAccessPolicy =
                    WidgetAccessPolicy(
                        channelIntegrationRepository = channelIntegrationRepository,
                        channelRepository = channelRepository,
                    ),
            )
    }

    @Test
    fun `bootstrap returns channel and integration for active widget and allowed origin`() {
        // Arrange: Prepare an active widget integration and active channel. / 준비: 활성 widget integration과 활성 channel을 준비한다.
        val integration = widgetIntegration(AllowedOrigins.of(listOf("https://acme.com")))
        every { channelIntegrationRepository.findByPublicKey("wpk_public") } returns integration
        every { channelRepository.findById("channel-1") } returns channel(ChannelStatus.ACTIVE)

        // Act: Resolve widget bootstrap data. / 실행: widget bootstrap data를 조회한다.
        val result =
            queryService.bootstrap(
                WidgetBootstrapQuery(
                    publicKey = "wpk_public",
                    origin = "https://acme.com/page",
                ),
            )

        // Assert: Verify channel and integration bootstrap data. / 검증: channel과 integration bootstrap data를 검증한다.
        result.channel.id shouldBe "channel-1"
        result.channel.name shouldBe "Acme"
        result.integration.id shouldBe integration.id
        result.integration.publicKey shouldBe "wpk_public"
    }

    @Test
    fun `bootstrap allows wildcard origin`() {
        // Arrange: Prepare an active widget integration with wildcard origin. / 준비: wildcard origin을 가진 활성 widget integration을 준비한다.
        val integration = widgetIntegration(AllowedOrigins.of(listOf("*")))
        every { channelIntegrationRepository.findByPublicKey("wpk_public") } returns integration
        every { channelRepository.findById("channel-1") } returns channel(ChannelStatus.ACTIVE)

        // Act: Resolve widget bootstrap data from any origin. / 실행: 임의 origin으로 widget bootstrap data를 조회한다.
        val result =
            queryService.bootstrap(
                WidgetBootstrapQuery(
                    publicKey = "wpk_public",
                    origin = "https://other.com",
                ),
            )

        // Assert: Verify wildcard origin allows bootstrap. / 검증: wildcard origin이 bootstrap을 허용하는지 검증한다.
        result.channel.id shouldBe "channel-1"
    }

    @Test
    fun `bootstrap throws when public key is unknown`() {
        // Arrange: Prepare an unknown public key lookup. / 준비: 알 수 없는 public key 조회를 준비한다.
        every { channelIntegrationRepository.findByPublicKey("missing") } returns null

        // Act: Try to resolve bootstrap data. / 실행: bootstrap data 조회를 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                queryService.bootstrap(
                    WidgetBootstrapQuery(
                        publicKey = "missing",
                        origin = "https://acme.com",
                    ),
                )
            }

        // Assert: Verify missing integration is reported. / 검증: integration 없음으로 보고되는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_INTEGRATION_NOT_FOUND
    }

    @Test
    fun `bootstrap throws when integration is disabled`() {
        // Arrange: Prepare a disabled widget integration. / 준비: 비활성 widget integration을 준비한다.
        val integration = widgetIntegration(AllowedOrigins.of(listOf("*"))).disable()
        every { channelIntegrationRepository.findByPublicKey("wpk_public") } returns integration

        // Act: Try to resolve bootstrap data. / 실행: bootstrap data 조회를 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                queryService.bootstrap(
                    WidgetBootstrapQuery(
                        publicKey = "wpk_public",
                        origin = "https://acme.com",
                    ),
                )
            }

        // Assert: Verify disabled integration is rejected. / 검증: 비활성 integration이 거부되는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_INTEGRATION_DISABLED
    }

    @Test
    fun `bootstrap throws when origin is denied`() {
        // Arrange: Prepare an integration that allows a different origin. / 준비: 다른 origin만 허용하는 integration을 준비한다.
        val integration = widgetIntegration(AllowedOrigins.of(listOf("https://acme.com")))
        every { channelIntegrationRepository.findByPublicKey("wpk_public") } returns integration

        // Act: Try to resolve bootstrap data from a denied origin. / 실행: 거부된 origin으로 bootstrap data 조회를 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                queryService.bootstrap(
                    WidgetBootstrapQuery(
                        publicKey = "wpk_public",
                        origin = "https://other.com",
                    ),
                )
            }

        // Assert: Verify origin policy rejects the request. / 검증: origin 정책이 요청을 거부하는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_INTEGRATION_ORIGIN_DENIED
    }

    @Test
    fun `bootstrap throws when channel is inactive`() {
        // Arrange: Prepare an active integration connected to an inactive channel. / 준비: 비활성 channel에 연결된 활성 integration을 준비한다.
        val integration = widgetIntegration(AllowedOrigins.of(listOf("*")))
        every { channelIntegrationRepository.findByPublicKey("wpk_public") } returns integration
        every { channelRepository.findById("channel-1") } returns channel(ChannelStatus.INACTIVE)

        // Act: Try to resolve bootstrap data. / 실행: bootstrap data 조회를 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                queryService.bootstrap(
                    WidgetBootstrapQuery(
                        publicKey = "wpk_public",
                        origin = "https://acme.com",
                    ),
                )
            }

        // Assert: Verify inactive channels are rejected. / 검증: 비활성 channel이 거부되는지 검증한다.
        exception.error shouldBe ConversationError.CHANNEL_NOT_ACTIVE
    }

    @Test
    fun `bootstrap throws when origin is invalid`() {
        // Arrange: Prepare an invalid origin input. / 준비: 유효하지 않은 origin 입력을 준비한다.
        val query =
            WidgetBootstrapQuery(
                publicKey = "wpk_public",
                origin = "not-an-origin",
            )

        // Act: Try to resolve bootstrap data. / 실행: bootstrap data 조회를 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                queryService.bootstrap(query)
            }

        // Assert: Verify invalid origin is rejected before lookup. / 검증: 유효하지 않은 origin이 조회 전에 거부되는지 검증한다.
        exception.error shouldBe ConversationError.INVALID_WIDGET_ORIGIN
    }

    private fun widgetIntegration(allowedOrigins: AllowedOrigins): ChannelIntegration =
        ChannelIntegration.createWidget(
            channelId = "channel-1",
            publicKey = "wpk_public",
            secretHash = "hashed-secret",
            allowedOrigins = allowedOrigins,
        )

    private fun channel(status: ChannelStatus): Channel =
        Channel(
            id = "channel-1",
            name = "Acme",
            status = status,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )
}
