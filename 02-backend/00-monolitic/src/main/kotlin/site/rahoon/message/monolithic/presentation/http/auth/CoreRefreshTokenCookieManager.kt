package site.rahoon.message.monolithic.presentation.http.auth

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

@ConfigurationProperties(prefix = "presentation.admin-auth.refresh-cookie")
data class CoreRefreshTokenCookieProperties(
    val name: String = "admin_refresh_token",
    val path: String = "/admin/auth",
    val sameSite: String = "Lax",
    val secure: Boolean = false,
    val ttlSeconds: Long = 1209600,
)

@Component
class CoreRefreshTokenCookieManager(
    private val properties: CoreRefreshTokenCookieProperties,
) {
    /**
     * Reads the refresh token from the configured cookie.
     */
    fun read(request: HttpServletRequest): String? =
        request.cookies
            ?.firstOrNull { it.name == properties.name }
            ?.value

    /**
     * Writes the refresh token cookie.
     */
    fun write(
        response: HttpServletResponse,
        refreshToken: String,
    ) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            buildCookie(refreshToken, Duration.ofSeconds(properties.ttlSeconds)).toString(),
        )
    }

    /**
     * Clears the refresh token cookie.
     */
    fun clear(response: HttpServletResponse) {
        response.addCookie(Cookie(properties.name, "").apply { maxAge = 0 })
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie("", Duration.ZERO).toString())
    }

    /**
     * Builds the refresh token response cookie.
     */
    private fun buildCookie(
        value: String,
        maxAge: Duration,
    ): ResponseCookie =
        ResponseCookie
            .from(properties.name, value)
            .httpOnly(true)
            .secure(properties.secure)
            .path(properties.path)
            .sameSite(properties.sameSite)
            .maxAge(maxAge)
            .build()
}
