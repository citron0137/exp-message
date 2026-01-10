package site.rahoon.message.__monolitic.authtoken.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.domain.DomainException
import site.rahoon.message.__monolitic.user.domain.UserRepository
import site.rahoon.message.__monolitic.user.domain.component.UserPasswordHasher

/**
 * 인증 토큰 도메인 서비스
 * 토큰 발급, 검증, 갱신 등의 비즈니스 로직을 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class AuthTokenDomainService {

    @Transactional
    fun login(command: AuthTokenCommand.Login): AuthToken {
        TODO("로그인 로직 구현")
    }

    @Transactional
    fun refresh(command: AuthTokenCommand.Refresh): AuthToken {
        TODO("토큰 갱신 로직 구현")
    }

    @Transactional
    fun logout(command: AuthTokenCommand.Logout) {
        TODO("로그아웃 로직 구현")
    }
}

