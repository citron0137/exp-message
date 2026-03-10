package site.rahoon.message.monolithic.authtoken.controller

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.authtoken.domain.RefreshToken
import java.time.Duration
import java.time.LocalDateTime

/**
 * Admin web refresh token cookie manager.
 */
@Component
class AdminWebRefreshTokenCookieManager(
    @Value("\${authtoken.web.refresh-cookie-name:admin_refresh_token}")
    private val cookieName: String,
    @Value("\${authtoken.web.refresh-cookie-path:/admin/web/auth}")
    private val cookiePath: String,
    @Value("\${authtoken.web.refresh-cookie-same-site:Lax}")
    private val sameSite: String,
    @Value("\${authtoken.web.refresh-cookie-secure:false}")
    private val forceSecureCookie: Boolean,
) {
    fun readRefreshToken(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == cookieName }
            ?.value
            ?.takeIf { it.isNotBlank() }

    fun setRefreshToken(
        response: HttpServletResponse,
        refreshToken: RefreshToken,
        request: HttpServletRequest,
    ) {
        val maxAgeSeconds = Duration.between(LocalDateTime.now(), refreshToken.expiresAt).seconds.coerceAtLeast(0)
        val cookie =
            ResponseCookie.from(cookieName, refreshToken.token)
                .httpOnly(true)
                .secure(resolveSecure(request))
                .path(cookiePath)
                .sameSite(sameSite)
                .maxAge(maxAgeSeconds)
                .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    fun clearRefreshToken(
        response: HttpServletResponse,
        request: HttpServletRequest,
    ) {
        val cookie =
            ResponseCookie.from(cookieName, "")
                .httpOnly(true)
                .secure(resolveSecure(request))
                .path(cookiePath)
                .sameSite(sameSite)
                .maxAge(0)
                .build()

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())
    }

    fun cookieName(): String = cookieName

    private fun resolveSecure(request: HttpServletRequest): Boolean = forceSecureCookie || request.isSecure
}
