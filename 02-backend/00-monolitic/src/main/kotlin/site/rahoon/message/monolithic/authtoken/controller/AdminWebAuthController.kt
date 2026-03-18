package site.rahoon.message.monolithic.authtoken.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import site.rahoon.message.monolithic.authtoken.application.AuthTokenApplicationService
import site.rahoon.message.monolithic.common.auth.CommonAdminAuthInfo
import site.rahoon.message.monolithic.common.controller.CommonApiResponse
import site.rahoon.message.monolithic.common.controller.component.IpAddressUtils
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException

/**
 * Admin web auth controller.
 * refresh token은 cookie로 처리합니다.
 */
@RestController
@RequestMapping("/admin/web/auth")
class AdminWebAuthController(
    private val authTokenApplicationService: AuthTokenApplicationService,
    private val refreshTokenCookieManager: AdminWebRefreshTokenCookieManager,
) {
    @PostMapping("/login")
    fun loginWithLock(
        @Valid @RequestBody request: AuthRequest.Login,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): CommonApiResponse<AdminWebAuthResponse.Login> {
        val ipAddress = IpAddressUtils.getClientIpAddress(httpRequest)
        val authToken =
            authTokenApplicationService.loginWithLock(
                email = request.email,
                password = request.password,
                ipAddress = ipAddress,
            )

        val refreshToken =
            authToken.refreshToken
                ?: throw DomainException(
                    error = CommonError.SERVER_ERROR,
                    details = mapOf("reason" to "refresh token was not issued"),
                )

        refreshTokenCookieManager.setRefreshToken(
            response = httpResponse,
            refreshToken = refreshToken,
            request = httpRequest,
        )

        val response = AdminWebAuthResponse.Login.from(authToken)
        return CommonApiResponse.success(response)
    }

    @PostMapping("/refresh")
    fun refresh(
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): CommonApiResponse<AdminWebAuthResponse.Login> {
        val refreshTokenString =
            refreshTokenCookieManager.readRefreshToken(httpRequest)
                ?: throw DomainException(
                    error = CommonError.UNAUTHORIZED,
                    details = mapOf("reason" to "Refresh token cookie is missing"),
                )

        val authToken = authTokenApplicationService.refresh(refreshTokenString)
        authToken.refreshToken?.let {
            refreshTokenCookieManager.setRefreshToken(
                response = httpResponse,
                refreshToken = it,
                request = httpRequest,
            )
        }

        val response = AdminWebAuthResponse.Login.from(authToken)
        return CommonApiResponse.success(response)
    }

    @PostMapping("/logout")
    fun logout(
        authInfo: CommonAdminAuthInfo,
        httpRequest: HttpServletRequest,
        httpResponse: HttpServletResponse,
    ): CommonApiResponse<AdminWebAuthResponse.Logout> {
        val sessionId =
            authInfo.sessionId
                ?: throw IllegalStateException("세션 ID가 없습니다")

        authTokenApplicationService.logout(sessionId)
        refreshTokenCookieManager.clearRefreshToken(httpResponse, httpRequest)

        val response = AdminWebAuthResponse.Logout()
        return CommonApiResponse.success(response)
    }
}
