package site.rahoon.message.__monolitic.authtoken.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import site.rahoon.message.__monolitic.user.controller.UserRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Auth Controller E2E 테스트
 * Redis Testcontainers를 사용하여 전체 스택 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthControllerE2ETest {

    companion object {
        @Container
        @JvmStatic
        val redisContainer = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379) // expose 6379 to random host port

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) } // get mapped port from random host port
            registry.add("spring.data.redis.password") { "" }
        }
    }

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun authBaseUrl(): String = "http://localhost:$port/auth"
    private fun userBaseUrl(): String = "http://localhost:$port/users"

    @BeforeEach
    fun setUp() {
        // 테스트용 사용자 생성
        val signUpRequest = UserRequest.SignUp(
            email = "test@example.com",
            password = "password123",
            nickname = "testuser"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(signUpRequest)
        val entity = HttpEntity(requestBody, headers)

        // 사용자가 없을 수 있으므로 회원가입 시도 (이미 존재하면 무시)
        try {
            restTemplate.exchange(
                userBaseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java
            )
        } catch (e: Exception) {
            // 이미 존재하는 경우 무시
        }
    }

    @Test
    fun `로그인 성공`() {
        // given
        val request = AuthRequest.Login(
            email = "test@example.com",
            password = "password123"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/login",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        // 응답 검증
        assertTrue(responseMap["success"] as Boolean)
        assertNotNull(responseMap["data"])

        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        assertNotNull(dataMap["accessToken"])
        assertNotNull(dataMap["accessTokenExpiresAt"])
        assertNotNull(dataMap["refreshToken"])
        assertNotNull(dataMap["refreshTokenExpiresAt"])
        assertNotNull(dataMap["userId"])
        assertNotNull(dataMap["sessionId"])
    }

    @Test
    fun `로그인 실패 - 잘못된 이메일`() {
        // given
        val request = AuthRequest.Login(
            email = "wrong@example.com",
            password = "password123"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/login",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])

        val errorMap = responseMap["error"] as? Map<*, *>
        assertEquals("USER_001", errorMap?.get("code")) // USER_NOT_FOUND
    }

    @Test
    fun `로그인 실패 - 잘못된 비밀번호`() {
        // given
        val request = AuthRequest.Login(
            email = "test@example.com",
            password = "wrongpassword"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/login",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])

        val errorMap = responseMap["error"] as? Map<*, *>
        assertEquals("USER_001", errorMap?.get("code")) // USER_NOT_FOUND (보안상 동일한 에러)
    }

    @Test
    fun `로그인 실패 - 이메일 형식 오류`() {
        // given
        val request = AuthRequest.Login(
            email = "invalid-email",
            password = "password123"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/login",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])

        val errorMap = responseMap["error"] as? Map<*, *>
        assertEquals("VALIDATION_ERROR", errorMap?.get("code"))
        assertNotNull(errorMap?.get("details"))
    }

    @Test
    fun `로그인 실패 - 필수 필드 누락`() {
        // given
        val request = mapOf(
            "email" to "test@example.com"
            // password 누락
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/login",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])

        val errorMap = responseMap["error"] as? Map<*, *>
        // 필수 필드 누락은 HttpMessageNotReadableException 또는 Validation 오류로 처리
        assertTrue(
            errorMap?.get("code") == "BAD_REQUEST" || errorMap?.get("code") == "VALIDATION_ERROR"
        )
    }

    @Test
    fun `로그인 실패 - 빈 이메일`() {
        // given
        val request = AuthRequest.Login(
            email = "",
            password = "password123"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/login",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])

        val errorMap = responseMap["error"] as? Map<*, *>
        assertEquals("VALIDATION_ERROR", errorMap?.get("code"))
        assertNotNull(errorMap?.get("details"))
    }
}

