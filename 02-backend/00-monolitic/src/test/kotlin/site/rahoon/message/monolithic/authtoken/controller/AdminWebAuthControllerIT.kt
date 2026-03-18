package site.rahoon.message.monolithic.authtoken.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import site.rahoon.message.monolithic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import site.rahoon.message.monolithic.common.test.assertError
import site.rahoon.message.monolithic.common.test.assertSuccess
import site.rahoon.message.monolithic.user.application.UserApplicationITUtils

/**
 * Admin web auth integration tests.
 */
class AdminWebAuthControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val userApplicationITUtils: UserApplicationITUtils,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    override val logger = KotlinLogging.logger { }
    private val refreshCookieName = "admin_refresh_token"

    private fun authBaseUrl(): String = "http://localhost:$port/admin/web/auth"

    private lateinit var testEmail: String
    private lateinit var testPassword: String

    @BeforeEach
    fun setUp() {
        val signUpResult = userApplicationITUtils.signUp()
        testEmail = signUpResult.email
        testPassword = signUpResult.password
    }

    @Test
    fun `웹 로그인 성공 - refresh token cookie 설정`() {
        // Arrange
        val request = AuthRequest.Login(email = testEmail, password = testPassword)
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("X-Forwarded-For", uniqueIp())
            }
        val entity = jsonEntity(request, headers)

        // Act
        val response = post("${authBaseUrl()}/login", entity)

        // Assert
        response.assertSuccess<AdminWebAuthResponse.Login>(objectMapper, HttpStatus.OK) { data ->
            data.accessToken shouldNotBe null
            data.userId shouldNotBe null
            data.sessionId shouldNotBe null
            data.role shouldNotBe null
        }

        val setCookie = setCookieHeader(response.headers)
        setCookie.contains("$refreshCookieName=") shouldBe true
        setCookie.contains("HttpOnly") shouldBe true
        setCookie.contains("Path=/admin/web/auth") shouldBe true
    }

    @Test
    fun `웹 refresh 성공 - cookie로 토큰 갱신`() {
        // Arrange
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set(HttpHeaders.COOKIE, "$refreshCookieName=${authResult.refreshToken}")
            }
        val entity = HttpEntity("{}", headers)

        // Act
        val response = post("${authBaseUrl()}/refresh", entity)

        // Assert
        response.assertSuccess<AdminWebAuthResponse.Login>(objectMapper, HttpStatus.OK) { data ->
            data.accessToken shouldNotBe null
            data.role shouldBe "ADMIN"
            data.accessToken shouldNotBe authResult.accessToken
        }

        val setCookie = setCookieHeader(response.headers)
        setCookie.contains("$refreshCookieName=") shouldBe true
    }

    @Test
    fun `웹 refresh 실패 - cookie 누락`() {
        // Arrange
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
            }
        val entity = HttpEntity("{}", headers)

        // Act
        val response = post("${authBaseUrl()}/refresh", entity)

        // Assert
        response.assertError(objectMapper, HttpStatus.UNAUTHORIZED, "COMMON_005")
    }

    @Test
    fun `웹 logout 성공 - refresh cookie 삭제`() {
        // Arrange
        val authResult = authApplicationITUtils.signUpAdminAndLogin()
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                set("Authorization", "Bearer ${authResult.accessToken}")
                set(HttpHeaders.COOKIE, "$refreshCookieName=${authResult.refreshToken}")
            }
        val entity = HttpEntity("{}", headers)

        // Act
        val response = post("${authBaseUrl()}/logout", entity)

        // Assert
        response.assertSuccess<AdminWebAuthResponse.Logout>(objectMapper, HttpStatus.OK) { data ->
            data.message shouldBe "로그아웃되었습니다"
        }

        val setCookie = setCookieHeader(response.headers)
        setCookie.contains("$refreshCookieName=") shouldBe true
        setCookie.contains("Max-Age=0") shouldBe true
    }

    private fun post(
        url: String,
        entity: HttpEntity<String>,
    ) = restTemplate.exchange(url, HttpMethod.POST, entity, String::class.java)

    private fun jsonEntity(
        body: Any,
        headers: HttpHeaders,
    ): HttpEntity<String> = HttpEntity(objectMapper.writeValueAsString(body), headers)

    private fun setCookieHeader(headers: HttpHeaders): String = headers[HttpHeaders.SET_COOKIE]?.joinToString(";") ?: ""
}
