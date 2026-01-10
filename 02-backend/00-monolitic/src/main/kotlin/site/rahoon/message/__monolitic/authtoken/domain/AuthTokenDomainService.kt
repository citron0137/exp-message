package site.rahoon.message.__monolitic.authtoken.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.authtoken.domain.component.JwtAccessTokenIssuer
import java.util.UUID

/**
 * 인증 토큰 도메인 서비스
 * 토큰 발급, 검증, 갱신 등의 비즈니스 로직을 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class AuthTokenDomainService(
    private val accessTokenIssuer: JwtAccessTokenIssuer
) {

    @Transactional
    fun issue(command: AuthTokenCommand.Issue): AuthToken {
        // NOTE: 사용자 검증/패스워드 검증은 application 레이어에서 수행합니다.
        // 여기서는 "검증된 주체"에 대한 토큰 발급만 책임집니다.
        val issuedAccessToken = accessTokenIssuer.issue(command.userId)

        val refreshToken = "refresh.${command.userId}.${UUID.randomUUID()}"
        return AuthToken.create(
            accessToken = issuedAccessToken.token,
            refreshToken = refreshToken,
            expiresAt = issuedAccessToken.expiresAt
        )
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

