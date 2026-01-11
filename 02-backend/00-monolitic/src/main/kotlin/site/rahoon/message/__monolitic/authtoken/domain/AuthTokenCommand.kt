package site.rahoon.message.__monolitic.authtoken.domain

sealed class AuthTokenCommand {
    data class Issue(
        val userId: String,
        val sessionId: String? = null
    ) : AuthTokenCommand()

    data class Refresh(
        val refreshToken: String
    ) : AuthTokenCommand()

    data class Logout(
        val sessionId: String
    ) : AuthTokenCommand()

    data class VerifyAccessToken(
        val accessToken: String
    ) : AuthTokenCommand()
}

