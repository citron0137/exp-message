package site.rahoon.message.monolithic.core.conversation.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.conversation.application.facade.CreateWidgetVisitorSessionCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.EnterWidgetConversationCommand
import site.rahoon.message.monolithic.core.conversation.application.facade.VisitorSessionProperties
import site.rahoon.message.monolithic.core.conversation.application.facade.WidgetEntryFacade
import site.rahoon.message.monolithic.core.conversation.application.port.ChannelConversationRepository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorRepository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionRepository
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionTokenGenerator
import site.rahoon.message.monolithic.core.conversation.application.port.VisitorSessionTokenHasher
import site.rahoon.message.monolithic.core.conversation.application.service.VisitorSessionPolicy
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccess
import site.rahoon.message.monolithic.core.conversation.application.service.WidgetAccessPolicy
import site.rahoon.message.monolithic.core.conversation.domain.AllowedOrigins
import site.rahoon.message.monolithic.core.conversation.domain.Channel
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversation
import site.rahoon.message.monolithic.core.conversation.domain.ChannelConversationStatus
import site.rahoon.message.monolithic.core.conversation.domain.ChannelIntegration
import site.rahoon.message.monolithic.core.conversation.domain.ChannelStatus
import site.rahoon.message.monolithic.core.conversation.domain.Origin
import site.rahoon.message.monolithic.core.conversation.domain.Visitor
import site.rahoon.message.monolithic.core.conversation.domain.VisitorSession
import site.rahoon.message.monolithic.core.conversation.exception.ConversationError
import site.rahoon.message.monolithic.core.conversation.exception.ConversationException
import java.time.LocalDateTime

class WidgetEntryFacadeUT {
    private lateinit var widgetAccessPolicy: WidgetAccessPolicy
    private lateinit var visitorRepository: VisitorRepository
    private lateinit var visitorSessionRepository: VisitorSessionRepository
    private lateinit var channelConversationRepository: ChannelConversationRepository
    private lateinit var visitorSessionTokenGenerator: VisitorSessionTokenGenerator
    private lateinit var visitorSessionTokenHasher: VisitorSessionTokenHasher
    private lateinit var visitorSessionPolicy: VisitorSessionPolicy
    private lateinit var facade: WidgetEntryFacade

    @BeforeEach
    fun setUp() {
        widgetAccessPolicy = mockk()
        visitorRepository = mockk()
        visitorSessionRepository = mockk()
        channelConversationRepository = mockk()
        visitorSessionTokenGenerator = mockk()
        visitorSessionTokenHasher = mockk()
        visitorSessionPolicy = mockk()
        facade =
            WidgetEntryFacade(
                widgetAccessPolicy = widgetAccessPolicy,
                visitorRepository = visitorRepository,
                visitorSessionRepository = visitorSessionRepository,
                channelConversationRepository = channelConversationRepository,
                visitorSessionTokenGenerator = visitorSessionTokenGenerator,
                visitorSessionTokenHasher = visitorSessionTokenHasher,
                visitorSessionPolicy = visitorSessionPolicy,
                visitorSessionProperties = VisitorSessionProperties(ttlSeconds = 604800),
            )
    }

    @Test
    fun `createVisitorSession creates visitor and returns raw session token`() {
        // Arrange: Prepare accessible widget and token collaborators. / 준비: 접근 가능한 widget과 token collaborator를 준비한다.
        every { widgetAccessPolicy.requireAccessibleWidget("wpk_public", "https://acme.com") } returns widgetAccess()
        every { visitorRepository.save(any()) } answers { firstArg() }
        every { visitorSessionTokenGenerator.generate() } returns "wvs_raw"
        every { visitorSessionTokenHasher.hash("wvs_raw") } returns "hashed-token"
        every { visitorSessionRepository.save(any()) } answers { firstArg() }

        // Act: Create a visitor session. / 실행: visitor session을 생성한다.
        val result =
            facade.createVisitorSession(
                CreateWidgetVisitorSessionCommand(
                    publicKey = "wpk_public",
                    origin = "https://acme.com",
                    externalId = "external-1",
                    displayName = "Alice",
                    email = "alice@example.com",
                    metadata = mapOf("plan" to "pro"),
                ),
            )

        // Assert: Verify visitor data and one-time raw token. / 검증: visitor data와 1회 raw token을 검증한다.
        result.visitor.channelId shouldBe "channel-1"
        result.visitor.externalId shouldBe "external-1"
        result.visitor.metadata shouldBe mapOf("plan" to "pro")
        result.session.token shouldBe "wvs_raw"
        verify { visitorSessionRepository.save(match { it.tokenHash == "hashed-token" }) }
    }

    @Test
    fun `enterConversation reuses existing open conversation`() {
        // Arrange: Prepare a valid session and an existing open conversation. / 준비: 유효한 session과 기존 open conversation을 준비한다.
        val visitor = visitor()
        val session = visitorSession(visitorId = visitor.id, expiresAt = LocalDateTime.now().plusDays(1))
        val existingConversation = ChannelConversation.start("channel-1", visitor.id).markOpen()
        every { widgetAccessPolicy.requireAccessibleWidget("wpk_public", "https://acme.com") } returns widgetAccess()
        every { visitorSessionPolicy.requireValidSession("wvs_raw", "channel-1") } returns session
        every { visitorRepository.findById(visitor.id) } returns visitor
        every { channelConversationRepository.findReusableByChannelIdAndVisitorId("channel-1", visitor.id) } returns existingConversation
        every { visitorSessionRepository.save(any()) } answers { firstArg() }

        // Act: Enter the widget conversation. / 실행: widget conversation에 진입한다.
        val result =
            facade.enterConversation(
                EnterWidgetConversationCommand(
                    publicKey = "wpk_public",
                    origin = "https://acme.com",
                    visitorSessionToken = "wvs_raw",
                ),
            )

        // Assert: Verify the existing open conversation is reused. / 검증: 기존 open conversation이 재사용되는지 검증한다.
        result.conversation.id shouldBe existingConversation.id
        verify(exactly = 0) { channelConversationRepository.save(any()) }
        verify { visitorSessionRepository.save(match { it.id == session.id }) }
    }

    @Test
    fun `enterConversation creates pending conversation when no reusable conversation exists`() {
        // Arrange: Prepare a valid session with no reusable conversation. / 준비: 재사용 가능한 conversation이 없는 유효한 session을 준비한다.
        val visitor = visitor()
        val session = visitorSession(visitorId = visitor.id, expiresAt = LocalDateTime.now().plusDays(1))
        every { widgetAccessPolicy.requireAccessibleWidget("wpk_public", "https://acme.com") } returns widgetAccess()
        every { visitorSessionPolicy.requireValidSession("wvs_raw", "channel-1") } returns session
        every { visitorRepository.findById(visitor.id) } returns visitor
        every { channelConversationRepository.findReusableByChannelIdAndVisitorId("channel-1", visitor.id) } returns null
        every { channelConversationRepository.save(any()) } answers { firstArg() }
        every { visitorSessionRepository.save(any()) } answers { firstArg() }

        // Act: Enter the widget conversation. / 실행: widget conversation에 진입한다.
        val result =
            facade.enterConversation(
                EnterWidgetConversationCommand(
                    publicKey = "wpk_public",
                    origin = "https://acme.com",
                    visitorSessionToken = "wvs_raw",
                ),
            )

        // Assert: Verify a new pending conversation is created. / 검증: 새 pending conversation이 생성되는지 검증한다.
        result.conversation.channelId shouldBe "channel-1"
        result.conversation.visitorId shouldBe visitor.id
        result.conversation.status shouldBe ChannelConversationStatus.PENDING
        verify {
            channelConversationRepository.save(
                match { it.status == ChannelConversationStatus.PENDING },
            )
        }
    }

    @Test
    fun `enterConversation reactivates dormant conversation as pending`() {
        // Arrange: Prepare a valid session and an existing dormant conversation. / 준비: 유효한 session과 기존 dormant conversation을 준비한다.
        val visitor = visitor()
        val session = visitorSession(visitorId = visitor.id, expiresAt = LocalDateTime.now().plusDays(1))
        val dormantConversation = ChannelConversation.start("channel-1", visitor.id).markDormant()
        every { widgetAccessPolicy.requireAccessibleWidget("wpk_public", "https://acme.com") } returns widgetAccess()
        every { visitorSessionPolicy.requireValidSession("wvs_raw", "channel-1") } returns session
        every { visitorRepository.findById(visitor.id) } returns visitor
        every { channelConversationRepository.findReusableByChannelIdAndVisitorId("channel-1", visitor.id) } returns dormantConversation
        every { channelConversationRepository.save(any()) } answers { firstArg() }
        every { visitorSessionRepository.save(any()) } answers { firstArg() }

        // Act: Enter the widget conversation. / 실행: widget conversation에 진입한다.
        val result =
            facade.enterConversation(
                EnterWidgetConversationCommand(
                    publicKey = "wpk_public",
                    origin = "https://acme.com",
                    visitorSessionToken = "wvs_raw",
                ),
            )

        // Assert: Verify dormant conversation is reactivated as pending. / 검증: dormant conversation이 pending으로 재활성화되는지 검증한다.
        result.conversation.id shouldBe dormantConversation.id
        result.conversation.status shouldBe ChannelConversationStatus.PENDING
        verify {
            channelConversationRepository.save(
                match {
                    it.id == dormantConversation.id &&
                        it.status == ChannelConversationStatus.PENDING
                },
            )
        }
    }

    @Test
    fun `enterConversation throws when visitor session is expired`() {
        // Arrange: Prepare an expired visitor session. / 준비: 만료된 visitor session을 준비한다.
        every { widgetAccessPolicy.requireAccessibleWidget("wpk_public", "https://acme.com") } returns widgetAccess()
        every { visitorSessionPolicy.requireValidSession("wvs_raw", "channel-1") } throws
            ConversationException(ConversationError.VISITOR_SESSION_EXPIRED)

        // Act: Try to enter a conversation with the expired session. / 실행: 만료된 session으로 conversation 진입을 시도한다.
        val exception =
            shouldThrow<ConversationException> {
                facade.enterConversation(
                    EnterWidgetConversationCommand(
                        publicKey = "wpk_public",
                        origin = "https://acme.com",
                        visitorSessionToken = "wvs_raw",
                    ),
                )
            }

        // Assert: Verify expired sessions are rejected. / 검증: 만료된 session이 거부되는지 검증한다.
        exception.error shouldBe ConversationError.VISITOR_SESSION_EXPIRED
    }

    private fun widgetAccess(): WidgetAccess =
        WidgetAccess(
            channel = channel(),
            integration =
                ChannelIntegration.createWidget(
                    channelId = "channel-1",
                    publicKey = "wpk_public",
                    secretHash = "secret-hash",
                    allowedOrigins = AllowedOrigins.of(listOf("*")),
                ),
            origin = Origin("https://acme.com"),
        )

    private fun channel(): Channel =
        Channel(
            id = "channel-1",
            name = "Acme",
            status = ChannelStatus.ACTIVE,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun visitor(): Visitor =
        Visitor.create(
            channelId = "channel-1",
            externalId = "external-1",
            displayName = "Alice",
            email = "alice@example.com",
            metadata = emptyMap(),
        )

    private fun visitorSession(
        visitorId: String,
        expiresAt: LocalDateTime,
    ): VisitorSession =
        VisitorSession.create(
            visitorId = visitorId,
            channelId = "channel-1",
            tokenHash = "hashed-token",
            expiresAt = expiresAt,
        )
}
