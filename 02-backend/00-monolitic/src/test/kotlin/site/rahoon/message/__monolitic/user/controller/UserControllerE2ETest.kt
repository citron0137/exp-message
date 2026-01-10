package site.rahoon.message.__monolitic.user.controller

import com.fasterxml.jackson.databind.ObjectMapper
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
import site.rahoon.message.__monolitic.common.controller.ApiResponse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * User Controller E2E 테스트
 * 회원가입 API에 대한 전체 스택 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerE2ETest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private fun baseUrl(): String = "http://localhost:$port/users"

    @Test
    fun `회원가입 성공`() {
        // given
        val request = UserRequest.SignUp(
            email = "test@example.com",
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
        assertEquals("test@example.com", dataMap["email"])
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
        val request = UserRequest.SignUp(
            email = "test@example.com",
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
        val request = UserRequest.SignUp(
            email = "test@example.com",
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
}

