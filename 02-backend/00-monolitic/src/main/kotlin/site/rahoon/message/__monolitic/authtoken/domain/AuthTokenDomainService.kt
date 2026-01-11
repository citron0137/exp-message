package site.rahoon.message.__monolitic.authtoken.domain

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.authtoken.domain.component.JwtAccessTokenIssuer
import site.rahoon.message.__monolitic.authtoken.domain.component.JwtAccessTokenClaimsExtractor
import java.time.Clock

@Service
@Transactional(readOnly = true)
class AuthTokenDomainService{

    @Transactional
    fun issue(command: AuthTokenCommand.Issue): AuthToken {
        TODO()
    }

    @Transactional
    fun refresh(command: AuthTokenCommand.Refresh): AuthToken {
        TODO()
    }

    @Transactional
    fun logout(command: AuthTokenCommand.Logout) {
        TODO()
    }

    fun verifyAccessToken(command: AuthTokenCommand.VerifyAccessToken): AccessToken {
        TODO()
    }
}

