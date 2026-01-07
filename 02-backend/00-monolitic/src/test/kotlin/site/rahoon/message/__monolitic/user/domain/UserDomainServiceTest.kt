package site.rahoon.message.__monolitic.user.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.user.domain.component.BCryptUserPasswordHasher
import site.rahoon.message.__monolitic.user.domain.component.UserCreateValidatorImpl
import site.rahoon.message.__monolitic.user.domain.component.UserUpdateValidatorImpl
import site.rahoon.message.__monolitic.user.infrastructure.UserJpaRepository
import site.rahoon.message.__monolitic.user.infrastructure.UserRepositoryImpl

@DataJpaTest
@Import(
    UserRepositoryImpl::class,
    BCryptUserPasswordHasher::class,
    UserCreateValidatorImpl::class,
    UserUpdateValidatorImpl::class,
    UserDomainService::class
)
class UserDomainServiceTest {

    @Autowired
    private lateinit var jpaRepository: UserJpaRepository

    @Autowired
    private lateinit var userRepository: UserRepositoryImpl

    @Autowired
    private lateinit var passwordHasher: BCryptUserPasswordHasher

    @Autowired
    private lateinit var userCreateValidator: UserCreateValidatorImpl

    @Autowired
    private lateinit var userUpdateValidator: UserUpdateValidatorImpl

    private lateinit var userDomainService: UserDomainService

    @BeforeEach
    fun setUp() {
        userDomainService = UserDomainService(
            userRepository,
            passwordHasher,
            userCreateValidator,
            userUpdateValidator
        )
    }

    @Test
    fun `사용자 생성 성공`() {
        // given
        val command = UserCommand.Create(
            email = "test@example.com",
            password = "password123",
            nickname = "testuser"
        )

        // when
        val userInfo = userDomainService.create(command)

        // then
        assertNotNull(userInfo.id)
        assertEquals("test@example.com", userInfo.email)
        assertEquals("testuser", userInfo.nickname)
        assertNotNull(userInfo.createdAt)
        assertNotNull(userInfo.updatedAt)
        // passwordHash는 UserInfo.Detail에 포함되지 않음
    }

    @Test
    fun `이메일 중복 시 예외 발생`() {
        // given
        val command1 = UserCommand.Create(
            email = "duplicate@example.com",
            password = "password123",
            nickname = "user1"
        )
        val command2 = UserCommand.Create(
            email = "duplicate@example.com",
            password = "password456",
            nickname = "user2"
        )

        // when
        userDomainService.create(command1)

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.create(command2)
        }
        assertEquals(UserError.EMAIL_ALREADY_EXISTS, exception.error)
    }

    @Test
    fun `사용자 닉네임 업데이트 성공`() {
        // given
        val createCommand = UserCommand.Create(
            email = "update@example.com",
            password = "password123",
            nickname = "oldnickname"
        )
        val userInfo = userDomainService.create(createCommand)
        val updateCommand = UserCommand.Update(
            id = userInfo.id,
            nickname = "newnickname"
        )

        // when
        val updatedUserInfo = userDomainService.update(updateCommand)

        // then
        assertEquals("newnickname", updatedUserInfo.nickname)
        assertEquals(userInfo.id, updatedUserInfo.id)
        assertEquals(userInfo.email, updatedUserInfo.email)
        assertTrue(updatedUserInfo.updatedAt.isAfter(userInfo.updatedAt))
    }

    @Test
    fun `존재하지 않는 사용자 업데이트 시 예외 발생`() {
        // given
        val updateCommand = UserCommand.Update(
            id = "non-existent-id",
            nickname = "newnickname"
        )

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.update(updateCommand)
        }
        assertEquals(UserError.USER_NOT_FOUND, exception.error)
    }

    @Test
    fun `사용자 삭제 성공`() {
        // given
        val createCommand = UserCommand.Create(
            email = "delete@example.com",
            password = "password123",
            nickname = "todelete"
        )
        val userInfo = userDomainService.create(createCommand)
        val deleteCommand = UserCommand.Delete(id = userInfo.id)

        // when
        val deletedUserInfo = userDomainService.delete(deleteCommand)

        // then
        assertEquals(userInfo.id, deletedUserInfo.id)
        assertNull(userRepository.findById(userInfo.id))
    }

    @Test
    fun `존재하지 않는 사용자 삭제 시 예외 발생`() {
        // given
        val deleteCommand = UserCommand.Delete(id = "non-existent-id")

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.delete(deleteCommand)
        }
        assertEquals(UserError.USER_NOT_FOUND, exception.error)
    }

    @Test
    fun `이메일이 비어있을 때 예외 발생`() {
        // given
        val command = UserCommand.Create(
            email = "",
            password = "password123",
            nickname = "testuser"
        )

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.create(command)
        }
        assertEquals(UserError.INVALID_EMAIL, exception.error)
    }

    @Test
    fun `잘못된 이메일 형식일 때 예외 발생`() {
        // given
        val command = UserCommand.Create(
            email = "invalid-email",
            password = "password123",
            nickname = "testuser"
        )

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.create(command)
        }
        assertEquals(UserError.INVALID_EMAIL, exception.error)
    }

    @Test
    fun `비밀번호가 8자 미만일 때 예외 발생`() {
        // given
        val command = UserCommand.Create(
            email = "test@example.com",
            password = "short",
            nickname = "testuser"
        )

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.create(command)
        }
        assertEquals(UserError.INVALID_PASSWORD, exception.error)
    }

    @Test
    fun `닉네임이 2자 미만일 때 예외 발생`() {
        // given
        val command = UserCommand.Create(
            email = "test@example.com",
            password = "password123",
            nickname = "a"
        )

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.create(command)
        }
        assertEquals(UserError.INVALID_NICKNAME, exception.error)
    }

    @Test
    fun `닉네임이 20자 초과일 때 예외 발생`() {
        // given
        val command = UserCommand.Create(
            email = "test@example.com",
            password = "password123",
            nickname = "a".repeat(21)
        )

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.create(command)
        }
        assertEquals(UserError.INVALID_NICKNAME, exception.error)
    }

    @Test
    fun `업데이트 시 닉네임이 비어있을 때 예외 발생`() {
        // given
        val createCommand = UserCommand.Create(
            email = "update@example.com",
            password = "password123",
            nickname = "testuser"
        )
        val userInfo = userDomainService.create(createCommand)
        val updateCommand = UserCommand.Update(
            id = userInfo.id,
            nickname = ""
        )

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.update(updateCommand)
        }
        assertEquals(UserError.INVALID_NICKNAME, exception.error)
    }

    @Test
    fun `업데이트 시 닉네임이 2자 미만일 때 예외 발생`() {
        // given
        val createCommand = UserCommand.Create(
            email = "update@example.com",
            password = "password123",
            nickname = "testuser"
        )
        val userInfo = userDomainService.create(createCommand)
        val updateCommand = UserCommand.Update(
            id = userInfo.id,
            nickname = "a"
        )

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.update(updateCommand)
        }
        assertEquals(UserError.INVALID_NICKNAME, exception.error)
    }

    @Test
    fun `업데이트 시 닉네임이 20자 초과일 때 예외 발생`() {
        // given
        val createCommand = UserCommand.Create(
            email = "update@example.com",
            password = "password123",
            nickname = "testuser"
        )
        val userInfo = userDomainService.create(createCommand)
        val updateCommand = UserCommand.Update(
            id = userInfo.id,
            nickname = "a".repeat(21)
        )

        // then
        val exception = assertThrows(DomainException::class.java) {
            userDomainService.update(updateCommand)
        }
        assertEquals(UserError.INVALID_NICKNAME, exception.error)
    }
}

