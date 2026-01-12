package site.rahoon.message.__monolitic.user.controller

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
import org.slf4j.LoggerFactory
import site.rahoon.message.__monolitic.authtoken.controller.AuthRequest
import site.rahoon.message.__monolitic.common.controller.ApiResponse
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * User Controller E2E 테스트
 * 회원가입 및 사용자 정보 조회 API에 대한 전체 스택 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class UserControllerE2ETest {

    companion object {
        @Container
        @JvmStatic
        val redisContainer = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "" }
        }
    }

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private val logger = LoggerFactory.getLogger(UserControllerE2ETest::class.java)

    private fun baseUrl(): String = "http://localhost:$port/users"
    private fun authBaseUrl(): String = "http://localhost:$port/auth"

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
                baseUrl(),
                HttpMethod.POST,
                entity,
                String::class.java
            )
        } catch (e: Exception) {
            // 이미 존재하는 경우 무시
        }
    }

    /**
     * 로그인하여 액세스 토큰을 받아옵니다.
     */
    private fun loginAndGetToken(email: String = "test@example.com", password: String = "password123"): String {
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
        
        val accessToken = dataMap?.get("accessToken") as? String
        assertNotNull(accessToken, "액세스 토큰이 null입니다")
        return accessToken!!
    }

    @Test
    fun `회원가입 성공`() {
        // given
        val uniqueEmail = "test-${UUID.randomUUID()}@example.com"
        val request = UserRequest.SignUp(
            email = uniqueEmail,
            password = "password123",
            nickname = "testuser"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        // 응답 검증
        assertTrue(responseMap["success"] as Boolean)
        assertNotNull(responseMap["data"])
        
        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        assertEquals(uniqueEmail, dataMap["email"])
        assertEquals("testuser", dataMap["nickname"])
        assertNotNull(dataMap["id"])
        assertNotNull(dataMap["createdAt"])
    }

    @Test
    fun `회원가입 실패 - 이메일 형식 오류`() {
        // given
        val request = UserRequest.SignUp(
            email = "invalid-email",
            password = "password123",
            nickname = "testuser"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
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
    fun `회원가입 실패 - 비밀번호 길이 부족`() {
        // given
        val uniqueEmail = "test-${UUID.randomUUID()}@example.com"
        val request = UserRequest.SignUp(
            email = uniqueEmail,
            password = "short",
            nickname = "testuser"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
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
        
        val details = errorMap?.get("details") as? Map<*, *>
        assertNotNull(details)
        assertTrue(details.containsKey("password"))
    }

    @Test
    fun `회원가입 실패 - 닉네임 길이 부족`() {
        // given
        val uniqueEmail = "test-${UUID.randomUUID()}@example.com"
        val request = UserRequest.SignUp(
            email = uniqueEmail,
            password = "password123",
            nickname = "a"
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
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
        
        val details = errorMap?.get("details") as? Map<*, *>
        assertNotNull(details)
        assertTrue(details.containsKey("nickname"))
    }

    @Test
    fun `회원가입 실패 - 필수 필드 누락`() {
        // given
        val request = mapOf(
            "email" to "test@example.com"
            // password와 nickname 누락
        )

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        // when
        val response = restTemplate.exchange(
            baseUrl(),
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
        // 필수 필드 누락은 HttpMessageNotReadableException으로 처리되어 BAD_REQUEST 반환
        assertEquals("BAD_REQUEST", errorMap?.get("code"))
    }

    @Test
    fun `현재 사용자 정보 조회 성공`() {
        // given
        val accessToken = loginAndGetToken()
        assertNotNull(accessToken, "액세스 토큰이 null입니다")
        logger.info("액세스 토큰: $accessToken")
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/me",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        if (response.statusCode != HttpStatus.OK) {
            logger.error("예상치 못한 상태 코드: ${response.statusCode}, 응답: ${response.body}")
        }
        assertEquals(HttpStatus.OK, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        // 응답 검증
        assertTrue(responseMap["success"] as Boolean)
        assertNotNull(responseMap["data"])

        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        assertEquals("test@example.com", dataMap["email"])
        assertEquals("testuser", dataMap["nickname"])
        assertNotNull(dataMap["id"])
        assertNotNull(dataMap["createdAt"])
        assertNotNull(dataMap["updatedAt"])
    }

    @Test
    fun `현재 사용자 정보 조회 실패 - 토큰 없음`() {
        // given
        val headers = HttpHeaders()
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/me",
            HttpMethod.GET,
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
        assertEquals("COMMON_005", errorMap?.get("code"))
    }

    @Test
    fun `현재 사용자 정보 조회 실패 - 잘못된 토큰`() {
        // given
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer invalid-token")
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/me",
            HttpMethod.GET,
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
        assertEquals("COMMON_005", errorMap?.get("code"))
    }
}

