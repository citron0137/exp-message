package site.rahoon.message.monolithic.presentation.http.auth

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.core.iam.access.application.facade.AccessFacade
import site.rahoon.message.monolithic.core.iam.access.application.facade.LoginCommand
import site.rahoon.message.monolithic.core.iam.access.application.facade.RefreshCommand
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthSessionResult
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.exception.AccessError
import site.rahoon.message.monolithic.core.iam.exception.AccessException
import site.rahoon.message.monolithic.presentation.http.shared.ApiResponse
import java.time.LocalDateTime

@RestController
@RequestMapping("/admin/auth")
class CoreAdminAuthController(
    private val accessFacade: AccessFacade,
    private val refreshTokenCookieManager: CoreRefreshTokenCookieManager,
) {
    /**
     * Logs in an admin user and issues access and refresh tokens.
     */
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: AdminAuthRequest.Login,
        httpResponse: HttpServletResponse,
    ): ApiResponse<AdminAuthResponse.Login> {
        val session =
            accessFacade.login(
                LoginCommand(
                    email = request.email,
                    password = request.password,
                ),
            )
        refreshTokenCookieManager.write(httpResponse, session.refreshToken)
        return ApiResponse.success(AdminAuthResponse.Login.from(session))
    }

    /**
     * Refreshes an admin session from cookie or request body refresh token.
     */
    @PostMapping("/refresh")
    fun refresh(
        @RequestBody(required = false) request: AdminAuthRequest.Refresh?,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): ApiResponse<AdminAuthResponse.Login> {
        val refreshToken =
            request?.refreshToken
                ?: refreshTokenCookieManager.read(httpRequest)
                ?: throw AccessException(AccessError.REFRESH_TOKEN_NOT_FOUND)
        val session = accessFacade.refresh(RefreshCommand(refreshToken))
        refreshTokenCookieManager.write(httpResponse, session.refreshToken)
        return ApiResponse.success(AdminAuthResponse.Login.from(session))
    }

    /**
     * Logs out an admin session and clears the refresh token cookie.
     */
    @PostMapping("/logout")
    fun logout(
        principal: AuthenticatedPrincipal,
        httpResponse: HttpServletResponse,
    ): ApiResponse<AdminAuthResponse.Logout> {
        accessFacade.logout(principal)
        refreshTokenCookieManager.clear(httpResponse)
        return ApiResponse.success(AdminAuthResponse.Logout())
    }
}

object AdminAuthRequest {
    data class Login(
        @field:Email
        @field:NotBlank
        val email: String,
        @field:NotBlank
        val password: String,
    )

    data class Refresh(
        val refreshToken: String?,
    )
}

object AdminAuthResponse {
    data class Login(
        val accessToken: String,
        val accessTokenExpiresAt: LocalDateTime,
        val refreshToken: String,
        val refreshTokenExpiresAt: LocalDateTime,
        val userId: String,
        val sessionId: String,
        val globalRole: String,
    ) {
        companion object {
            /**
             * Maps an auth session result to a login response.
             */
            fun from(session: AuthSessionResult): Login =
                Login(
                    accessToken = session.accessToken.token,
                    accessTokenExpiresAt = session.accessToken.expiresAt,
                    refreshToken = session.refreshToken,
                    refreshTokenExpiresAt = session.refreshTokenExpiresAt,
                    userId = session.accessToken.principal.userId,
                    sessionId = session.accessToken.principal.sessionId,
                    globalRole = session.accessToken.principal.globalRole.name,
                )
        }
    }

    data class Logout(
        val loggedOut: Boolean = true,
    )
}
