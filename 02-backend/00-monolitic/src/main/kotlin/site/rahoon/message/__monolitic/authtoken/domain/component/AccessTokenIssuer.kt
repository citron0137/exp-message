package site.rahoon.message.__monolitic.authtoken.domain.component

import java.time.LocalDateTime

interface AccessTokenIssuer {
    fun issue(userId: String): IssuedAccessToken
}

data class IssuedAccessToken(
    val token: String,
    val expiresAt: LocalDateTime
)

