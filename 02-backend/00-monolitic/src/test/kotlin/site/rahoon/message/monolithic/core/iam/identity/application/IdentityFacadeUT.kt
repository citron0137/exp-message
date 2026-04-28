package site.rahoon.message.monolithic.core.iam.identity.application

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import site.rahoon.message.monolithic.core.iam.identity.application.facade.CreateOrLoadCustomerAdminCommand
import site.rahoon.message.monolithic.core.iam.identity.application.facade.CreatePlatformAdminIfAbsentCommand
import site.rahoon.message.monolithic.core.iam.identity.application.facade.IdentityFacade
import site.rahoon.message.monolithic.core.iam.identity.application.port.IdentityUserRepository
import site.rahoon.message.monolithic.core.iam.identity.application.port.PasswordHasher
import site.rahoon.message.monolithic.core.iam.identity.application.service.TemporaryPasswordGenerator
import site.rahoon.message.monolithic.core.iam.identity.domain.GlobalRole
import site.rahoon.message.monolithic.core.iam.identity.domain.IdentityUser

class IdentityFacadeUT {
    private lateinit var identityUserRepository: IdentityUserRepository
    private lateinit var passwordHasher: PasswordHasher
    private lateinit var temporaryPasswordGenerator: TemporaryPasswordGenerator
    private lateinit var facade: IdentityFacade

    @BeforeEach
    fun setUp() {
        identityUserRepository = mockk()
        passwordHasher = mockk()
        temporaryPasswordGenerator = mockk()
        facade =
            IdentityFacade(
                identityUserRepository = identityUserRepository,
                passwordHasher = passwordHasher,
                temporaryPasswordGenerator = temporaryPasswordGenerator,
            )
    }

    @Test
    fun `createPlatformAdminIfAbsent returns not created when platform admin exists`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        every { identityUserRepository.existsByGlobalRole(GlobalRole.PLATFORM_ADMIN) } returns true

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result =
            facade.createPlatformAdminIfAbsent(
                CreatePlatformAdminIfAbsentCommand(
                    email = "admin@example.com",
                    password = "password",
                    nickname = "Admin",
                ),
            )

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result.created shouldBe false
        result.temporaryPassword.shouldBeNull()
        verify(exactly = 0) { passwordHasher.hash(any()) }
        verify(exactly = 0) { temporaryPasswordGenerator.generate() }
        verify(exactly = 0) { identityUserRepository.save(any()) }
    }

    @Test
    fun `createPlatformAdminIfAbsent creates platform admin with supplied password`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        every { identityUserRepository.existsByGlobalRole(GlobalRole.PLATFORM_ADMIN) } returns false
        every { passwordHasher.hash("password") } returns "hashed-password"
        every { identityUserRepository.save(any()) } answers { firstArg() }

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result =
            facade.createPlatformAdminIfAbsent(
                CreatePlatformAdminIfAbsentCommand(
                    email = "admin@example.com",
                    password = "password",
                    nickname = "Admin",
                ),
            )

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result.created shouldBe true
        result.temporaryPassword.shouldBeNull()
        verify {
            identityUserRepository.save(
                match {
                    it.email == "admin@example.com" &&
                        it.passwordHash == "hashed-password" &&
                        it.nickname == "Admin" &&
                        it.globalRole == GlobalRole.PLATFORM_ADMIN
                },
            )
        }
        verify(exactly = 0) { temporaryPasswordGenerator.generate() }
    }

    @Test
    fun `createPlatformAdminIfAbsent creates temporary password when password is blank`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        every { identityUserRepository.existsByGlobalRole(GlobalRole.PLATFORM_ADMIN) } returns false
        every { temporaryPasswordGenerator.generate() } returns "temporary-password"
        every { passwordHasher.hash("temporary-password") } returns "hashed-temporary-password"
        every { identityUserRepository.save(any()) } answers { firstArg() }

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result =
            facade.createPlatformAdminIfAbsent(
                CreatePlatformAdminIfAbsentCommand(
                    email = "admin@example.com",
                    password = "",
                    nickname = "Admin",
                ),
            )

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result.created shouldBe true
        result.temporaryPassword shouldBe "temporary-password"
        verify { passwordHasher.hash("temporary-password") }
        verify { identityUserRepository.save(any()) }
    }

    @Test
    fun `createOrLoadCustomerAdmin returns existing identity by email`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        val existing =
            IdentityUser.create(
                email = "customer@example.com",
                passwordHash = "hashed-password",
                nickname = "Customer",
                globalRole = GlobalRole.CHANNEL_USER,
            )
        every { identityUserRepository.findByEmail("customer@example.com") } returns existing

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result =
            facade.createOrLoadCustomerAdmin(
                CreateOrLoadCustomerAdminCommand(
                    email = "customer@example.com",
                    nickname = "Customer",
                ),
            )

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result.userId shouldBe existing.id
        result.email shouldBe existing.email
        result.nickname shouldBe existing.nickname
        result.created shouldBe false
        result.temporaryPassword.shouldBeNull()
        verify(exactly = 0) { temporaryPasswordGenerator.generate() }
        verify(exactly = 0) { identityUserRepository.save(any()) }
    }

    @Test
    fun `createOrLoadCustomerAdmin creates channel user with generated temporary password`() {
        // Arrange: Prepare the required test data and dependencies. / 준비: 필요한 테스트 데이터와 의존성을 준비한다.
        every { identityUserRepository.findByEmail("customer@example.com") } returns null
        every { temporaryPasswordGenerator.generate() } returns "temporary-password"
        every { passwordHasher.hash("temporary-password") } returns "hashed-temporary-password"
        every { identityUserRepository.save(any()) } answers { firstArg() }

        // Act: Execute the behavior under test. / 실행: 테스트 대상 동작을 실행한다.
        val result =
            facade.createOrLoadCustomerAdmin(
                CreateOrLoadCustomerAdminCommand(
                    email = "customer@example.com",
                    nickname = "Customer",
                ),
            )

        // Assert: Verify the expected result and interactions. / 검증: 기대 결과와 상호작용을 검증한다.
        result.email shouldBe "customer@example.com"
        result.nickname shouldBe "Customer"
        result.created shouldBe true
        result.temporaryPassword shouldBe "temporary-password"
        verify {
            identityUserRepository.save(
                match {
                    it.email == "customer@example.com" &&
                        it.passwordHash == "hashed-temporary-password" &&
                        it.nickname == "Customer" &&
                        it.globalRole == GlobalRole.CHANNEL_USER
                },
            )
        }
    }
}
