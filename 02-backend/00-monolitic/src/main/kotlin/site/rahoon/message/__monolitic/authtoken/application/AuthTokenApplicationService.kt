package site.rahoon.message.__monolitic.authtoken.application

import org.springframework.stereotype.Service
import site.rahoon.message.__monolitic.authtoken.domain.AuthToken
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenCommand
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenDomainService
import site.rahoon.message.__monolitic.authtoken.domain.AuthTokenError
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.user.domain.UserRepository
import site.rahoon.message.__monolitic.user.domain.component.UserPasswordHasher

/**
 * AuthToken Application Service
 * application 레이어에서 User/AuthToken 도메인을 조합해 로그인 플로우를 구성합니다.
 *
 * - email/password 검증: User 도메인(Repository/Hasher)을 사용
 * - 토큰 발급: AuthToken 도메인에 "검증된 userId"만 전달
 */
@Service
class AuthTokenApplicationService(
    private val userRepository: UserRepository,
    private val passwordHasher: UserPasswordHasher,
    private val authTokenDomainService: AuthTokenDomainService
) {
    fun login(criteria: AuthTokenCriteria.Login): AuthToken {
        val user = userRepository.findByEmail(criteria.email)
            ?: throw DomainException(
                error = AuthTokenError.INVALID_CREDENTIALS,
                details = mapOf("email" to criteria.email)
            )

        val ok = passwordHasher.verify(criteria.password, user.passwordHash)
        if (!ok) {
            throw DomainException(
                error = AuthTokenError.INVALID_CREDENTIALS,
                details = mapOf("email" to criteria.email)
            )
        }

        return authTokenDomainService.issue(
            AuthTokenCommand.Issue(userId = user.id)
        )
    }
}

