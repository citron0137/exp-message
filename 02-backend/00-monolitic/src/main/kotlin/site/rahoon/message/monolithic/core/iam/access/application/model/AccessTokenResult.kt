package site.rahoon.message.monolithic.core.iam.access.application.model

import java.time.LocalDateTime

data class AccessTokenResult(
    val token: String,
    val expiresAt: LocalDateTime,
    val principal: AuthenticatedPrincipal,
)

data class AuthSessionResult(
    val accessToken: AccessTokenResult,
    val refreshToken: String,
    val refreshTokenExpiresAt: LocalDateTime,
)
