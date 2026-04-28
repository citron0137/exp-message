package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelIntegrationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelMembershipRepository
import site.rahoon.message.monolithic.core.conversation.application.query.AdminChannelIntegrationQueryService
import site.rahoon.message.monolithic.core.conversation.application.service.ChannelAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AllowedOrigins
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import java.time.LocalDateTime

class AdminChannelIntegrationQueryServiceUT {
    private lateinit var channelIntegrationRepository: ChannelIntegrationRepository
    private lateinit var channelMembershipRepository: ChannelMembershipRepository
    private lateinit var queryService: AdminChannelIntegrationQueryService

    @BeforeEach
    fun setUp() {
        channelIntegrationRepository = mockk()
        channelMembershipRepository = mockk()
        queryService =
            AdminChannelIntegrationQueryService(
                channelIntegrationRepository = channelIntegrationRepository,
                channelAccessPolicy = ChannelAccessPolicy(channelMembershipRepository),
            )
    }

    @Test
    fun `listByChannel returns mapped integrations for platform admin`() {
        // Arrange: Prepare a platform admin and channel integrations. / 준비: platform admin과 channel integrations를 준비한다.
        val actor = principal(PrincipalGlobalRole.PLATFORM_ADMIN)
        val integrations = listOf(widgetIntegration())
        every { channelIntegrationRepository.findByChannelId("channel-1") } returns integrations

        // Act: List integrations through the query service. / 실행: query service로 integrations를 조회한다.
        val result = queryService.listByChannel(actor, "channel-1")

        // Assert: Verify mapped integration data. / 검증: 매핑된 integration 데이터를 검증한다.
        result.shouldHaveSize(1)
        result[0].id shouldBe integrations[0].id
        result[0].channelId shouldBe "channel-1"
        result[0].allowedOrigins shouldBe listOf("https://acme.com")
    }

    @Test
    fun `listByChannel rejects non platform admin`() {
        // Arrange: Prepare a channel user actor. / 준비: channel user actor를 준비한다.
        val actor = principal(PrincipalGlobalRole.CHANNEL_USER)

        // Act: Try to list integrations without platform admin role. / 실행: platform admin 권한 없이 integrations 조회를 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                queryService.listByChannel(actor, "channel-1")
            }

        // Assert: Verify platform admin role is required. / 검증: platform admin 권한이 필요한지 검증한다.
        exception.error shouldBe ConversationError.PLATFORM_ADMIN_REQUIRED
    }

    private fun principal(globalRole: PrincipalGlobalRole): AuthenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = "user-1",
            sessionId = "session-1",
            globalRole = globalRole,
            expiresAt = LocalDateTime.now().plusHours(1),
        )

    private fun widgetIntegration(): ChannelIntegration =
        ChannelIntegration.createWidget(
            channelId = "channel-1",
            publicKey = "wpk_public",
            secretHash = "hashed-secret",
            allowedOrigins = AllowedOrigins.of(listOf("https://acme.com")),
        )
}
