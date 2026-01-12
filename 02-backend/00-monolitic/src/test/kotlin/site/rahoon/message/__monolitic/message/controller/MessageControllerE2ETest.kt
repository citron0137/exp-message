package site.rahoon.message.__monolitic.message.controller

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
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import org.slf4j.LoggerFactory
import site.rahoon.message.__monolitic.authtoken.controller.AuthRequest
import site.rahoon.message.__monolitic.chatroom.controller.ChatRoomRequest
import site.rahoon.message.__monolitic.user.controller.UserRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Message Controller E2E 테스트
 * 메시지 전송, 조회 API에 대한 전체 스택 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class MessageControllerE2ETest {

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

    private val logger = LoggerFactory.getLogger(MessageControllerE2ETest::class.java)

    private fun baseUrl(): String = "http://localhost:$port/messages"
    private fun authBaseUrl(): String = "http://localhost:$port/auth"
    private fun userBaseUrl(): String = "http://localhost:$port/users"
    private fun chatRoomBaseUrl(): String = "http://localhost:$port/chat-rooms"

    /**
     * 로그인하여 액세스 토큰을 받아옵니다.
     */
    private fun loginAndGetToken(email: String = "test@example.com", password: String = "password123"): String {
        // 먼저 회원가입 시도
        val signUpRequest = UserRequest.SignUp(
            email = email,
            password = password,
            nickname = "testuser"
        )

        val signUpHeaders = HttpHeaders()
        signUpHeaders.contentType = MediaType.APPLICATION_JSON
        val signUpRequestBody = objectMapper.writeValueAsString(signUpRequest)
        val signUpEntity = HttpEntity(signUpRequestBody, signUpHeaders)

        try {
            restTemplate.exchange(
                userBaseUrl(),
                HttpMethod.POST,
                signUpEntity,
                String::class.java
            )
        } catch (e: Exception) {
            // 이미 존재하는 경우 무시
        }

        // 로그인
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

        val accessToken = dataMap.get("accessToken") as? String
        assertNotNull(accessToken, "액세스 토큰이 null입니다")
        return accessToken
    }

    /**
     * 채팅방을 생성하고 ID를 반환합니다.
     */
    private fun createChatRoom(accessToken: String, name: String = "테스트 채팅방"): String {
        val request = ChatRoomRequest.Create(name = name)

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        headers.contentType = MediaType.APPLICATION_JSON
        val requestBody = objectMapper.writeValueAsString(request)
        val entity = HttpEntity(requestBody, headers)

        val response = restTemplate.exchange(
            chatRoomBaseUrl(),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        assertEquals(HttpStatus.CREATED, response.statusCode, "채팅방 생성 실패: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        val chatRoomId = dataMap["id"] as? String
        assertNotNull(chatRoomId)
        return chatRoomId
    }

    @Test
    fun `메시지 전송 성공`() {
        // given
        val accessToken = loginAndGetToken()
        val chatRoomId = createChatRoom(accessToken)
        val request = MessageRequest.Create(
            chatRoomId = chatRoomId,
            content = "안녕하세요!"
        )

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
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
        assertEquals(HttpStatus.CREATED, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(responseMap["success"] as Boolean)
        assertNotNull(responseMap["data"])

        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        assertEquals(chatRoomId, dataMap["chatRoomId"])
        assertEquals("안녕하세요!", dataMap["content"])
        assertNotNull(dataMap["id"])
        assertNotNull(dataMap["userId"])
        assertNotNull(dataMap["createdAt"])
    }

    @Test
    fun `메시지 전송 실패 - 인증 없음`() {
        // given
        val request = MessageRequest.Create(
            chatRoomId = "test-chatroom-id",
            content = "안녕하세요!"
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
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `메시지 전송 실패 - 채팅방이 존재하지 않음`() {
        // given
        val accessToken = loginAndGetToken()
        val request = MessageRequest.Create(
            chatRoomId = "non-existent-chatroom-id",
            content = "안녕하세요!"
        )

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
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
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])
    }

    @Test
    fun `메시지 전송 실패 - 채팅방 멤버가 아님`() {
        // given
        val accessToken1 = loginAndGetToken("user1@example.com", "password123")
        val accessToken2 = loginAndGetToken("user2@example.com", "password123")
        
        // user1이 채팅방 생성
        val chatRoomId = createChatRoom(accessToken1)
        
        // user2가 채팅방에 참가하지 않고 메시지 전송 시도
        val request = MessageRequest.Create(
            chatRoomId = chatRoomId,
            content = "안녕하세요!"
        )

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken2")
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
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])
    }

    @Test
    fun `메시지 전송 실패 - 내용 누락`() {
        // given
        val accessToken = loginAndGetToken()
        val chatRoomId = createChatRoom(accessToken)
        val request = mapOf<String, Any>(
            "chatRoomId" to chatRoomId
            // content 필드 누락
        )

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
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
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])
    }

    @Test
    fun `메시지 조회 성공`() {
        // given
        val accessToken = loginAndGetToken()
        val chatRoomId = createChatRoom(accessToken)
        
        // 먼저 메시지 전송
        val createRequest = MessageRequest.Create(
            chatRoomId = chatRoomId,
            content = "조회 테스트 메시지"
        )

        val createHeaders = HttpHeaders()
        createHeaders.set("Authorization", "Bearer $accessToken")
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createRequestBody = objectMapper.writeValueAsString(createRequest)
        val createEntity = HttpEntity(createRequestBody, createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            createEntity,
            String::class.java
        )

        assertEquals(HttpStatus.CREATED, createResponse.statusCode)
        @Suppress("UNCHECKED_CAST")
        val createResponseMap = objectMapper.readValue(createResponse.body!!, Map::class.java) as Map<String, Any>
        val createDataMap = createResponseMap["data"] as? Map<*, *>
        val messageId = createDataMap?.get("id") as? String
        assertNotNull(messageId)

        // when - 메시지 조회
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $accessToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val response = restTemplate.exchange(
            "${baseUrl()}/$messageId",
            HttpMethod.GET,
            getEntity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(responseMap["success"] as Boolean)
        assertNotNull(responseMap["data"])

        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        assertEquals(messageId, dataMap["id"])
        assertEquals("조회 테스트 메시지", dataMap["content"])
        assertEquals(chatRoomId, dataMap["chatRoomId"])
    }

    @Test
    fun `메시지 조회 실패 - 메시지가 존재하지 않음`() {
        // given
        val accessToken = loginAndGetToken()
        val nonExistentMessageId = "non-existent-message-id"

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/$nonExistentMessageId",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])
    }

    @Test
    fun `메시지 조회 실패 - 인증 없음`() {
        // given
        val messageId = "test-message-id"

        val headers = HttpHeaders()
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}/$messageId",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `채팅방별 메시지 목록 조회 성공`() {
        // given
        val accessToken = loginAndGetToken()
        val chatRoomId = createChatRoom(accessToken)

        // 메시지 2개 전송
        val message1 = MessageRequest.Create(chatRoomId = chatRoomId, content = "첫 번째 메시지")
        val message2 = MessageRequest.Create(chatRoomId = chatRoomId, content = "두 번째 메시지")

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        headers.contentType = MediaType.APPLICATION_JSON

        val entity1 = HttpEntity(objectMapper.writeValueAsString(message1), headers)
        val entity2 = HttpEntity(objectMapper.writeValueAsString(message2), headers)

        restTemplate.exchange(baseUrl(), HttpMethod.POST, entity1, String::class.java)
        restTemplate.exchange(baseUrl(), HttpMethod.POST, entity2, String::class.java)

        // when - 채팅방별 메시지 목록 조회
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $accessToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val response = restTemplate.exchange(
            "${baseUrl()}?chatRoomId=$chatRoomId",
            HttpMethod.GET,
            getEntity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(responseMap["success"] as Boolean)
        assertNotNull(responseMap["data"])

        val dataList = responseMap["data"] as? List<*>
        assertNotNull(dataList)
        assertTrue(dataList.size >= 2, "메시지가 2개 이상 있어야 합니다")

        // 최신순 정렬 확인 (나중에 보낸 메시지가 먼저)
        val firstMessage = dataList[0] as? Map<*, *>
        val secondMessage = dataList[1] as? Map<*, *>
        assertNotNull(firstMessage)
        assertNotNull(secondMessage)
        assertEquals("두 번째 메시지", firstMessage["content"])
        assertEquals("첫 번째 메시지", secondMessage["content"])
    }

    @Test
    fun `채팅방별 메시지 목록 조회 실패 - 채팅방이 존재하지 않음`() {
        // given
        val accessToken = loginAndGetToken()
        val nonExistentChatRoomId = "non-existent-chatroom-id"

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}?chatRoomId=$nonExistentChatRoomId",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])
    }

    @Test
    fun `채팅방별 메시지 목록 조회 실패 - 인증 없음`() {
        // given
        val chatRoomId = "test-chatroom-id"

        val headers = HttpHeaders()
        val entity = HttpEntity<Nothing?>(null, headers)

        // when
        val response = restTemplate.exchange(
            "${baseUrl()}?chatRoomId=$chatRoomId",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `채팅방별 메시지 목록 조회 - 빈 목록`() {
        // given
        val accessToken = loginAndGetToken()
        val chatRoomId = createChatRoom(accessToken)

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<Nothing?>(null, headers)

        // when - 메시지가 없는 채팅방 조회
        val response = restTemplate.exchange(
            "${baseUrl()}?chatRoomId=$chatRoomId",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(responseMap["success"] as Boolean)
        assertNotNull(responseMap["data"])

        val dataList = responseMap["data"] as? List<*>
        assertNotNull(dataList)
        assertTrue(dataList.isEmpty(), "메시지가 없어야 합니다")
    }
}
