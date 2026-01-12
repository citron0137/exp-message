package site.rahoon.message.__monolitic.chatroommember.controller

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
import site.rahoon.message.__monolitic.chatroom.controller.ChatRoomRequest
import site.rahoon.message.__monolitic.user.controller.UserRequest
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ChatRoomMember Controller E2E 테스트
 * 채팅방 참가, 나가기, 멤버 목록 조회 API에 대한 전체 스택 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ChatRoomMemberControllerE2ETest {

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

    private val logger = LoggerFactory.getLogger(ChatRoomMemberControllerE2ETest::class.java)

    private fun baseUrl(chatRoomId: String): String = "http://localhost:$port/chat-rooms/$chatRoomId/members"
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
        val chatRoomId = dataMap?.get("id") as? String
        assertNotNull(chatRoomId, "채팅방 ID가 null입니다")
        return chatRoomId
    }

    @Test
    fun `채팅방 참가 성공`() {
        // given
        val accessToken1 = loginAndGetToken("user1@example.com", "password123")
        val accessToken2 = loginAndGetToken("user2@example.com", "password123")
        val chatRoomId = createChatRoom(accessToken1, "참가 테스트 채팅방")

        // when - user2가 채팅방에 참가
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken2")
        val entity = HttpEntity<Nothing?>(null, headers)

        val response = restTemplate.exchange(
            baseUrl(chatRoomId),
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
        assertNotNull(dataMap["userId"])
        assertNotNull(dataMap["joinedAt"])
    }

    @Test
    fun `채팅방 참가 실패 - 이미 참가한 멤버`() {
        // given
        val accessToken = loginAndGetToken()
        val chatRoomId = createChatRoom(accessToken, "중복 참가 테스트 채팅방")

        // when - 이미 생성자로 자동 참가되어 있으므로 다시 참가 시도
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<Nothing?>(null, headers)

        val response = restTemplate.exchange(
            baseUrl(chatRoomId),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])
    }

    @Test
    fun `채팅방 참가 실패 - 존재하지 않는 채팅방`() {
        // given
        val accessToken = loginAndGetToken()
        val nonExistentChatRoomId = "non-existent-chat-room-id"

        // when
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<Nothing?>(null, headers)

        val response = restTemplate.exchange(
            baseUrl(nonExistentChatRoomId),
            HttpMethod.POST,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode, "응답: ${response.body}")
    }

    @Test
    fun `채팅방 나가기 성공`() {
        // given
        val accessToken1 = loginAndGetToken("user3@example.com", "password123")
        val accessToken2 = loginAndGetToken("user4@example.com", "password123")
        val chatRoomId = createChatRoom(accessToken1, "나가기 테스트 채팅방")

        // user2가 참가
        val joinHeaders = HttpHeaders()
        joinHeaders.set("Authorization", "Bearer $accessToken2")
        val joinEntity = HttpEntity<Nothing?>(null, joinHeaders)
        restTemplate.exchange(
            baseUrl(chatRoomId),
            HttpMethod.POST,
            joinEntity,
            String::class.java
        )

        // when - user2가 나가기
        val leaveHeaders = HttpHeaders()
        leaveHeaders.set("Authorization", "Bearer $accessToken2")
        val leaveEntity = HttpEntity<Nothing?>(null, leaveHeaders)

        val response = restTemplate.exchange(
            baseUrl(chatRoomId),
            HttpMethod.DELETE,
            leaveEntity,
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
        assertEquals(chatRoomId, dataMap["chatRoomId"])
    }

    @Test
    fun `채팅방 나가기 실패 - 멤버가 아님`() {
        // given
        val accessToken1 = loginAndGetToken("user5@example.com", "password123")
        val accessToken2 = loginAndGetToken("user6@example.com", "password123")
        val chatRoomId = createChatRoom(accessToken1, "나가기 실패 테스트 채팅방")

        // when - 참가하지 않은 user2가 나가기 시도
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken2")
        val entity = HttpEntity<Nothing?>(null, headers)

        val response = restTemplate.exchange(
            baseUrl(chatRoomId),
            HttpMethod.DELETE,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])
    }

    @Test
    fun `채팅방 멤버 목록 조회 성공`() {
        // given
        val accessToken1 = loginAndGetToken("user7@example.com", "password123")
        val accessToken2 = loginAndGetToken("user8@example.com", "password123")
        val accessToken3 = loginAndGetToken("user9@example.com", "password123")
        val chatRoomId = createChatRoom(accessToken1, "멤버 목록 테스트 채팅방")

        // user2, user3 참가
        val joinHeaders2 = HttpHeaders()
        joinHeaders2.set("Authorization", "Bearer $accessToken2")
        restTemplate.exchange(
            baseUrl(chatRoomId),
            HttpMethod.POST,
            HttpEntity<Nothing?>(null, joinHeaders2),
            String::class.java
        )

        val joinHeaders3 = HttpHeaders()
        joinHeaders3.set("Authorization", "Bearer $accessToken3")
        restTemplate.exchange(
            baseUrl(chatRoomId),
            HttpMethod.POST,
            HttpEntity<Nothing?>(null, joinHeaders3),
            String::class.java
        )

        // when - 멤버 목록 조회
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $accessToken1")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val response = restTemplate.exchange(
            baseUrl(chatRoomId),
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

        @Suppress("UNCHECKED_CAST")
        val dataList = responseMap["data"] as? List<Map<*, *>>
        assertNotNull(dataList)
        assertTrue(dataList.size >= 3, "멤버 수가 3명 이상이어야 합니다 (생성자 + user2 + user3)")
    }

    @Test
    fun `채팅방 생성 시 생성자가 자동으로 멤버로 추가됨`() {
        // given
        val accessToken = loginAndGetToken("user10@example.com", "password123")
        val chatRoomId = createChatRoom(accessToken, "자동 멤버 추가 테스트 채팅방")

        // when - 멤버 목록 조회
        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        val entity = HttpEntity<Nothing?>(null, headers)

        val response = restTemplate.exchange(
            baseUrl(chatRoomId),
            HttpMethod.GET,
            entity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(responseMap["success"] as Boolean)
        assertNotNull(responseMap["data"])

        @Suppress("UNCHECKED_CAST")
        val dataList = responseMap["data"] as? List<Map<*, *>>
        assertNotNull(dataList)
        assertEquals(1, dataList.size, "생성자가 자동으로 멤버로 추가되어야 합니다")
    }
}
