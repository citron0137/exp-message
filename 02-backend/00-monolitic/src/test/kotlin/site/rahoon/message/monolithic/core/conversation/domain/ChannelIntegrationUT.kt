package site.rahoon.message.monolithic.core.conversation.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test

class ChannelIntegrationUT {
    @Test
    fun `createWidget creates active widget integration`() {
        // Arrange: Prepare widget integration creation values. / 준비: widget integration 생성 값을 준비한다.
        val allowedOrigins = AllowedOrigins.of(listOf("https://acme.com"))

        // Act: Create a widget integration. / 실행: widget integration을 생성한다.
        val integration =
            ChannelIntegration.createWidget(
                channelId = "channel-1",
                publicKey = "wpk_public",
                secretHash = "hashed-secret",
                allowedOrigins = allowedOrigins,
            )

        // Assert: Verify widget defaults and generated state. / 검증: widget 기본값과 생성 상태를 검증한다.
        integration.id.shouldNotBeBlank()
        integration.channelId shouldBe "channel-1"
        integration.type shouldBe ChannelIntegrationType.WIDGET
        integration.publicKey shouldBe "wpk_public"
        integration.secretHash shouldBe "hashed-secret"
        integration.status shouldBe ChannelIntegrationStatus.ACTIVE
        integration.allowedOrigins shouldBe allowedOrigins
        integration.createdAt shouldBe integration.updatedAt
    }

    @Test
    fun `disable returns disabled integration`() {
        // Arrange: Prepare an active widget integration. / 준비: 활성 widget integration을 준비한다.
        val integration = widgetIntegration()

        // Act: Disable the integration. / 실행: integration을 비활성화한다.
        val disabled = integration.disable()

        // Assert: Verify the integration status is disabled. / 검증: integration 상태가 disabled인지 검증한다.
        disabled.status shouldBe ChannelIntegrationStatus.DISABLED
    }

    @Test
    fun `enable returns active integration`() {
        // Arrange: Prepare a disabled widget integration. / 준비: 비활성 widget integration을 준비한다.
        val integration = widgetIntegration().disable()

        // Act: Enable the integration. / 실행: integration을 활성화한다.
        val enabled = integration.enable()

        // Assert: Verify the integration status is active. / 검증: integration 상태가 active인지 검증한다.
        enabled.status shouldBe ChannelIntegrationStatus.ACTIVE
    }

    @Test
    fun `updateAllowedOrigins replaces allowed origins`() {
        // Arrange: Prepare an integration and replacement origins. / 준비: integration과 교체할 origins를 준비한다.
        val integration = widgetIntegration()
        val allowedOrigins = AllowedOrigins.of(listOf("*"))

        // Act: Replace allowed origins. / 실행: allowed origins를 교체한다.
        val updated = integration.updateAllowedOrigins(allowedOrigins)

        // Assert: Verify allowed origins were replaced. / 검증: allowed origins가 교체되었는지 검증한다.
        updated.allowedOrigins shouldBe allowedOrigins
    }

    private fun widgetIntegration(): ChannelIntegration =
        ChannelIntegration.createWidget(
            channelId = "channel-1",
            publicKey = "wpk_public",
            secretHash = "hashed-secret",
            allowedOrigins = AllowedOrigins.of(listOf("https://acme.com")),
        )
}
