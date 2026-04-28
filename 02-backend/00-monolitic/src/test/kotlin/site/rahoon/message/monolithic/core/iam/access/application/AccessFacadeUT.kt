package site.rahoon.message.monolithic.core.iam.access.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.iam.access.application.facade.AccessFacade
import site.rahoon.message.monolithic.core.iam.access.application.facade.LoginCommand
import site.rahoon.message.monolithic.core.iam.access.application.facade.RefreshCommand
import site.rahoon.message.monolithic.core.iam.access.application.model.AccessTokenResult
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.access.application.model.PrincipalGlobalRole
import site.rahoon.message.monolithic.core.iam.access.application.port.AccessPasswordVerifier
import site.rahoon.message.monolithic.core.iam.access.application.port.CoreRefreshTokenRepository
import site.rahoon.message.monolithic.core.iam.access.application.port.LoginPrincipalReader
import site.rahoon.message.monolithic.core.iam.access.application.port.LoginPrincipalSnapshot
import site.rahoon.message.monolithic.core.iam.access.application.service.CoreAccessTokenService
import site.rahoon.message.monolithic.core.iam.access.domain.CoreRefreshToken
import site.rahoon.message.monolithic.core.iam.exception.AccessError
import site.rahoon.message.monolithic.core.iam.exception.AccessException
import java.time.LocalDateTime

class AccessFacadeUT {
    private lateinit var loginPrincipalReader: LoginPrincipalReader
    private lateinit var passwordVerifier: AccessPasswordVerifier
    private lateinit var refreshTokenRepository: CoreRefreshTokenRepository
    private lateinit var accessTokenService: CoreAccessTokenService
    private lateinit var facade: AccessFacade

    @BeforeEach
    fun setUp() {
        loginPrincipalReader = mockk()
        passwordVerifier = mockk()
        refreshTokenRepository = mockk()
        accessTokenService = mockk()
        facade =
            AccessFacade(
                loginPrincipalReader = loginPrincipalReader,
                passwordVerifier = passwordVerifier,
                refreshTokenRepository = refreshTokenRepository,
                accessTokenService = accessTokenService,
            )
    }

    @Test
    fun `login issues access token and refresh token when credentials are valid`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val snapshot = loginPrincipalSnapshot()
        every { loginPrincipalReader.findByEmail("admin@example.com") } returns snapshot
        every { passwordVerifier.verify("password", "hashed-password") } returns true
        every { accessTokenService.issue(any()) } answers { accessToken(firstArg()) }
        every { accessTokenService.refreshTokenTtlSeconds() } returns 3600
        val savedRefreshToken = slot<CoreRefreshToken>()
        every { refreshTokenRepository.save(capture(savedRefreshToken)) } answers { savedRefreshToken.captured }

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result =
            facade.login(
                LoginCommand(
                    email = "admin@example.com",
                    password = "password",
                ),
            )

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result.accessToken.token shouldBe "access-token"
        result.accessToken.principal.userId shouldBe "user-1"
        result.accessToken.principal.globalRole shouldBe PrincipalGlobalRole.PLATFORM_ADMIN
        val sessionId = result.accessToken.principal.sessionId
        sessionId.shouldNotBeBlank()
        result.refreshToken shouldBe savedRefreshToken.captured.token
        result.refreshTokenExpiresAt shouldBe savedRefreshToken.captured.expiresAt
        savedRefreshToken.captured.userId shouldBe "user-1"
        savedRefreshToken.captured.sessionId shouldBe sessionId
    }

    @Test
    fun `login throws invalid credentials when email is unknown`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        every { loginPrincipalReader.findByEmail("missing@example.com") } returns null

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val exception =
            shouldThrow<AccessException> {
                facade.login(
                    LoginCommand(
                        email = "missing@example.com",
                        password = "password",
                    ),
                )
            }

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        exception.error shouldBe AccessError.INVALID_CREDENTIALS
        verify(exactly = 0) { passwordVerifier.verify(any(), any()) }
        verify(exactly = 0) { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `login throws invalid credentials when password does not match`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val snapshot = loginPrincipalSnapshot()
        every { loginPrincipalReader.findByEmail("admin@example.com") } returns snapshot
        every { passwordVerifier.verify("wrong-password", "hashed-password") } returns false

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val exception =
            shouldThrow<AccessException> {
                facade.login(
                    LoginCommand(
                        email = "admin@example.com",
                        password = "wrong-password",
                    ),
                )
            }

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        exception.error shouldBe AccessError.INVALID_CREDENTIALS
        verify(exactly = 0) { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `refresh rotates refresh token and issues a new session`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val existing =
            refreshToken(
                token = "old-refresh-token",
                sessionId = "old-session",
                expiresAt = LocalDateTime.now().plusMinutes(10),
            )
        every { refreshTokenRepository.findByToken("old-refresh-token") } returns existing
        every { refreshTokenRepository.deleteByToken("old-refresh-token") } returns Unit
        every { loginPrincipalReader.findById("user-1") } returns loginPrincipalSnapshot()
        every { accessTokenService.issue(any()) } answers { accessToken(firstArg()) }
        every { accessTokenService.refreshTokenTtlSeconds() } returns 3600
        every { refreshTokenRepository.save(any()) } answers { firstArg() }

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result = facade.refresh(RefreshCommand(refreshToken = "old-refresh-token"))

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result.accessToken.principal.userId shouldBe "user-1"
        result.accessToken.principal.globalRole shouldBe PrincipalGlobalRole.PLATFORM_ADMIN
        val sessionId = result.accessToken.principal.sessionId
        sessionId.shouldNotBeBlank()
        verify { refreshTokenRepository.deleteByToken("old-refresh-token") }
        verify {
            refreshTokenRepository.save(
                match {
                    it.userId == "user-1" &&
                        it.sessionId == result.accessToken.principal.sessionId
                },
            )
        }
    }

    @Test
    fun `refresh deletes expired token and throws refresh token expired`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val existing =
            refreshToken(
                token = "expired-refresh-token",
                sessionId = "expired-session",
                expiresAt = LocalDateTime.now().minusSeconds(1),
            )
        every { refreshTokenRepository.findByToken("expired-refresh-token") } returns existing
        every { refreshTokenRepository.deleteByToken("expired-refresh-token") } returns Unit

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val exception =
            shouldThrow<AccessException> {
                facade.refresh(RefreshCommand(refreshToken = "expired-refresh-token"))
            }

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        exception.error shouldBe AccessError.REFRESH_TOKEN_EXPIRED
        verify { refreshTokenRepository.deleteByToken("expired-refresh-token") }
        verify(exactly = 0) { refreshTokenRepository.save(any()) }
    }

    @Test
    fun `logout deletes refresh token by session id`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val principal = principal(sessionId = "session-1")
        every { refreshTokenRepository.deleteBySessionId("session-1") } returns Unit

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        facade.logout(principal)

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        verify { refreshTokenRepository.deleteBySessionId("session-1") }
    }

    @Test
    fun `verifyAccessToken delegates raw token verification`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val principal = principal(sessionId = "session-1")
        every { accessTokenService.verify("raw-access-token") } returns principal

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result = facade.verifyAccessToken("raw-access-token")

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result shouldBe principal
        verify { accessTokenService.verify("raw-access-token") }
    }

    private fun loginPrincipalSnapshot(): LoginPrincipalSnapshot =
        LoginPrincipalSnapshot(
            userId = "user-1",
            email = "admin@example.com",
            passwordHash = "hashed-password",
            globalRole = PrincipalGlobalRole.PLATFORM_ADMIN,
        )

    private fun accessToken(principal: AuthenticatedPrincipal): AccessTokenResult =
        AccessTokenResult(
            token = "access-token",
            expiresAt = principal.expiresAt.plusMinutes(30),
            principal = principal,
        )

    private fun principal(sessionId: String): AuthenticatedPrincipal =
        AuthenticatedPrincipal(
            userId = "user-1",
            sessionId = sessionId,
            globalRole = PrincipalGlobalRole.PLATFORM_ADMIN,
            expiresAt = LocalDateTime.now().plusHours(1),
        )

    private fun refreshToken(
        token: String,
        sessionId: String,
        expiresAt: LocalDateTime,
    ): CoreRefreshToken =
        CoreRefreshToken(
            token = token,
            userId = "user-1",
            sessionId = sessionId,
            expiresAt = expiresAt,
            createdAt = expiresAt.minusDays(1),
        )
}
