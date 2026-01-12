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

    /**
     * 로그인하여 AuthToken 정보를 받아옵니다.
     */
    private fun loginAndGetTokens(email: String = "test@example.com", password: String = "password123"): Map<String, Any> {
        val request = AuthRequest.Login(
            email = email,
            password = password
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        val response = restTemplate.exchange(
            "${authBaseUrl()}/login",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        assertEquals(HttpStatus.OK, response.statusCode, "로그인 실패: ${response.body}")
        assertNotNull(response.body, "로그인 응답이 null입니다")

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(responseMap["success"] as Boolean, "로그인 응답이 실패했습니다: ${response.body}")

        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap, "로그인 응답의 data가 null입니다")
        @Suppress("UNCHECKED_CAST")
        return dataMap as Map<String, Any>
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

    @Test
    fun `토큰 갱신 성공`() {
        // given
        val loginData = loginAndGetTokens()
        val refreshToken = loginData["refreshToken"] as? String
        assertNotNull(refreshToken, "리프레시 토큰이 null입니다")

        val request = AuthRequest.Refresh(
            refreshToken = refreshToken
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/refresh",
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

        // 새로운 토큰이 발급되었는지 확인 (기존 토큰과 다름)
        val newAccessToken = dataMap["accessToken"] as? String
        val oldAccessToken = loginData["accessToken"] as? String
        assertNotNull(newAccessToken)
        assertNotNull(oldAccessToken)
        assertTrue(newAccessToken != oldAccessToken, "새로운 액세스 토큰이 발급되어야 합니다")
    }

    @Test
    fun `토큰 갱신 실패 - 잘못된 리프레시 토큰`() {
        // given
        val request = AuthRequest.Refresh(
            refreshToken = "invalid-refresh-token"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/refresh",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode) // AuthTokenError.INVALID_TOKEN은 CLIENT_ERROR 타입이므로 BAD_REQUEST
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])

        val errorMap = responseMap["error"] as? Map<*, *>
        assertEquals("AUTH_003", errorMap?.get("code")) // INVALID_TOKEN
    }

    @Test
    fun `토큰 갱신 실패 - 리프레시 토큰 누락`() {
        // given
        val request = mapOf<String, Any>(
            // refreshToken 누락
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/refresh",
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
    fun `토큰 갱신 실패 - 빈 리프레시 토큰`() {
        // given
        val request = AuthRequest.Refresh(
            refreshToken = ""
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/refresh",
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
    fun `로그아웃 성공`() {
        // given
        val loginData = loginAndGetTokens()
        val accessToken = loginData["accessToken"] as? String
        assertNotNull(accessToken, "액세스 토큰이 null입니다")

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/logout",
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
        assertEquals("로그아웃되었습니다", dataMap["message"])

        // 로그아웃 후 리프레시 토큰이 만료되었는지 확인
        val refreshToken = loginData["refreshToken"] as? String
        assertNotNull(refreshToken)

        val refreshRequest = AuthRequest.Refresh(
            refreshToken = refreshToken
        )

        val refreshHeaders = HttpHeaders()
        refreshHeaders.contentType = MediaType.APPLICATION_JSON
        val refreshRequestBody = objectMapper.writeValueAsString(refreshRequest)
        val refreshEntity = HttpEntity(refreshRequestBody, refreshHeaders)

        val refreshResponse = restTemplate.exchange(
            "${authBaseUrl()}/refresh",
            HttpMethod.POST,
            refreshEntity,
            String::class.java
        )

        // 로그아웃 후에는 리프레시 토큰이 무효화되어야 함
        // AuthTokenError.INVALID_TOKEN은 CLIENT_ERROR 타입이므로 BAD_REQUEST 반환
        assertEquals(HttpStatus.BAD_REQUEST, refreshResponse.statusCode)
        assertNotNull(refreshResponse.body)

        @Suppress("UNCHECKED_CAST")
        val refreshResponseMap = objectMapper.readValue(refreshResponse.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(refreshResponseMap["success"] as Boolean))
        assertNotNull(refreshResponseMap["error"])

        val errorMap = refreshResponseMap["error"] as? Map<*, *>
        assertEquals("AUTH_003", errorMap?.get("code")) // INVALID_TOKEN
    }

    @Test
    fun `로그아웃 실패 - 토큰 없음`() {
        // given
        val headers = HttpHeaders()
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/logout",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])

        val errorMap = responseMap["error"] as? Map<*, *>
        assertEquals("COMMON_005", errorMap?.get("code")) // UNAUTHORIZED
    }

    @Test
    fun `로그아웃 실패 - 잘못된 토큰`() {
        // given
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer invalid-token")
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${authBaseUrl()}/logout",
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])

        val errorMap = responseMap["error"] as? Map<*, *>
        assertEquals("COMMON_005", errorMap?.get("code")) // UNAUTHORIZED
    }

    /**
     * 리프레시 토큰 만료 테스트 예시
     * 
     * TTL 만료 테스트를 하려면 다음과 같이 별도 테스트 클래스를 만들거나,
     * @DynamicPropertySource에서 TTL을 짧게 설정할 수 있습니다:
     * 
     * ```kotlin
     * @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
     * @Testcontainers
     * class AuthControllerExpirationE2ETest {
     *     companion object {
     *         @Container
     *         @JvmStatic
     *         val redisContainer = GenericContainer(DockerImageName.parse("redis:7-alpine"))
     *             .withExposedPorts(6379)
     * 
     *         @JvmStatic
     *         @DynamicPropertySource
     *         fun configureProperties(registry: DynamicPropertyRegistry) {
     *             registry.add("spring.data.redis.host") { redisContainer.host }
     *             registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
     *             registry.add("spring.data.redis.password") { "" }
     *             // TTL을 짧게 설정 (예: 2초)
     *             registry.add("authtoken.refresh-token-ttl-seconds") { "2" }
     *         }
     *     }
     * 
     *     @Test
     *     fun `토큰 갱신 실패 - 리프레시 토큰 만료`() {
     *         // given
     *         val loginData = loginAndGetTokens()
     *         val refreshToken = loginData["refreshToken"] as? String
     *         assertNotNull(refreshToken)
     * 
     *         // TTL 만료 대기 (예: 3초 대기)
     *         Thread.sleep(3000)
     * 
     *         // when
     *         val request = AuthRequest.Refresh(refreshToken = refreshToken)
     *         // ... refresh 요청
     * 
     *         // then
     *         // UNAUTHORIZED 또는 INVALID_TOKEN 에러 확인
     *     }
     * }
     * ```
     * 
     * 참고: 실제 프로덕션에서는 TTL이 길기 때문에(14일), 만료 테스트는 선택적으로 구현합니다.
     * 기본적인 refresh 동작 테스트는 현재 TTL로 충분합니다.
     */
}

