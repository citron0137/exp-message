package site.rahoon.message.__monolitic.chatroom.controller

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
import site.rahoon.message.__monolitic.user.controller.UserRequest
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ChatRoom Controller E2E 테스트
 * 채팅방 생성, 조회, 수정, 삭제 API에 대한 전체 스택 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ChatRoomControllerE2ETest {

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

    private val logger = LoggerFactory.getLogger(ChatRoomControllerE2ETest::class.java)

    private fun baseUrl(): String = "http://localhost:$port/chat-rooms"
    private fun authBaseUrl(): String = "http://localhost:$port/auth"
    private fun userBaseUrl(): String = "http://localhost:$port/users"

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

    @Test
    fun `채팅방 생성 성공`() {
        // given
        val accessToken = loginAndGetToken()
        val request = ChatRoomRequest.Create(
            name = "테스트 채팅방"
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
        assertEquals("테스트 채팅방", dataMap["name"])
        assertNotNull(dataMap["id"])
        assertNotNull(dataMap["createdByUserId"])
        assertNotNull(dataMap["createdAt"])
        assertNotNull(dataMap["updatedAt"])
    }

    @Test
    fun `채팅방 생성 실패 - 인증 없음`() {
        // given
        val request = ChatRoomRequest.Create(
            name = "테스트 채팅방"
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
    fun `채팅방 생성 실패 - 이름 누락`() {
        // given
        val accessToken = loginAndGetToken()
        val request = mapOf<String, Any>() // name 필드 누락

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
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])
    }

    @Test
    fun `채팅방 조회 성공`() {
        // given
        val accessToken = loginAndGetToken()
        
        // 먼저 채팅방 생성
        val createRequest = ChatRoomRequest.Create(
            name = "조회 테스트 채팅방"
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
        val chatRoomId = createDataMap?.get("id") as? String
        assertNotNull(chatRoomId)

        // when - 채팅방 조회
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $accessToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
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
        assertEquals(chatRoomId, dataMap["id"])
        assertEquals("조회 테스트 채팅방", dataMap["name"])
    }

    @Test
    fun `내가 참여한 채팅방 목록 조회 성공 - 생성한 채팅방`() {
        // given
        val accessToken = loginAndGetToken()
        
        // 채팅방 2개 생성 (생성 시 자동으로 멤버가 됨)
        val createRequest1 = ChatRoomRequest.Create(name = "채팅방 1")
        val createRequest2 = ChatRoomRequest.Create(name = "채팅방 2")

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $accessToken")
        headers.contentType = MediaType.APPLICATION_JSON

        val entity1 = HttpEntity(objectMapper.writeValueAsString(createRequest1), headers)
        val entity2 = HttpEntity(objectMapper.writeValueAsString(createRequest2), headers)

        val response1 = restTemplate.exchange(baseUrl(), HttpMethod.POST, entity1, String::class.java)
        val response2 = restTemplate.exchange(baseUrl(), HttpMethod.POST, entity2, String::class.java)

        @Suppress("UNCHECKED_CAST")
        val responseMap1 = objectMapper.readValue(response1.body!!, Map::class.java) as Map<String, Any>
        val responseMap2 = objectMapper.readValue(response2.body!!, Map::class.java) as Map<String, Any>
        val dataMap1 = responseMap1["data"] as? Map<*, *>
        val dataMap2 = responseMap2["data"] as? Map<*, *>
        val chatRoomId1 = dataMap1?.get("id") as? String
        val chatRoomId2 = dataMap2?.get("id") as? String
        assertNotNull(chatRoomId1)
        assertNotNull(chatRoomId2)

        // when - 내가 참여한 채팅방 목록 조회
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $accessToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val response = restTemplate.exchange(
            baseUrl(),
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
        assertTrue(dataList.size >= 2, "채팅방 목록이 2개 이상이어야 합니다")
        
        // 생성한 채팅방들이 목록에 포함되어 있는지 확인
        val chatRoomIds = dataList.mapNotNull { (it as? Map<*, *>)?.get("id") as? String }
        assertTrue(chatRoomIds.contains(chatRoomId1), "채팅방 1이 목록에 포함되어야 합니다")
        assertTrue(chatRoomIds.contains(chatRoomId2), "채팅방 2가 목록에 포함되어야 합니다")
    }

    @Test
    fun `내가 참여한 채팅방 목록 조회 성공 - 다른 사용자가 생성한 채팅방에 참여`() {
        // given
        val creatorToken = loginAndGetToken("creator3@example.com", "password123")
        val memberToken = loginAndGetToken("member3@example.com", "password123")
        
        // 생성자가 채팅방 생성
        val createRequest = ChatRoomRequest.Create(name = "참여할 채팅방")
        val createHeaders = HttpHeaders()
        createHeaders.set("Authorization", "Bearer $creatorToken")
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(objectMapper.writeValueAsString(createRequest), createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            createEntity,
            String::class.java
        )

        @Suppress("UNCHECKED_CAST")
        val createResponseMap = objectMapper.readValue(createResponse.body!!, Map::class.java) as Map<String, Any>
        val createDataMap = createResponseMap["data"] as? Map<*, *>
        val chatRoomId = createDataMap?.get("id") as? String
        assertNotNull(chatRoomId)

        // 다른 사용자가 채팅방에 참여
        val joinHeaders = HttpHeaders()
        joinHeaders.set("Authorization", "Bearer $memberToken")
        val joinEntity = HttpEntity<Nothing?>(null, joinHeaders)

        val joinResponse = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId/members",
            HttpMethod.POST,
            joinEntity,
            String::class.java
        )
        assertEquals(HttpStatus.CREATED, joinResponse.statusCode, "참여 실패: ${joinResponse.body}")

        // when - 참여한 사용자가 자신이 참여한 채팅방 목록 조회
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $memberToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val response = restTemplate.exchange(
            baseUrl(),
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
        assertTrue(dataList.isNotEmpty(), "참여한 채팅방이 목록에 있어야 합니다")
        
        // 참여한 채팅방이 목록에 포함되어 있는지 확인
        val chatRoomIds = dataList.mapNotNull { (it as? Map<*, *>)?.get("id") as? String }
        assertTrue(chatRoomIds.contains(chatRoomId), "참여한 채팅방이 목록에 포함되어야 합니다")
        
        // 채팅방 정보 확인
        val chatRoom = dataList.find { (it as? Map<*, *>)?.get("id") == chatRoomId } as? Map<*, *>
        assertNotNull(chatRoom)
        assertEquals("참여할 채팅방", chatRoom["name"])
    }

    @Test
    fun `내가 참여한 채팅방 목록 조회 성공 - 여러 채팅방에 참여`() {
        // given
        val userToken = loginAndGetToken("multi@example.com", "password123")
        val otherUserToken = loginAndGetToken("other3@example.com", "password123")
        
        // 다른 사용자가 채팅방 2개 생성
        val createRequest1 = ChatRoomRequest.Create(name = "채팅방 A")
        val createRequest2 = ChatRoomRequest.Create(name = "채팅방 B")

        val createHeaders = HttpHeaders()
        createHeaders.set("Authorization", "Bearer $otherUserToken")
        createHeaders.contentType = MediaType.APPLICATION_JSON

        val entity1 = HttpEntity(objectMapper.writeValueAsString(createRequest1), createHeaders)
        val entity2 = HttpEntity(objectMapper.writeValueAsString(createRequest2), createHeaders)

        val response1 = restTemplate.exchange(baseUrl(), HttpMethod.POST, entity1, String::class.java)
        val response2 = restTemplate.exchange(baseUrl(), HttpMethod.POST, entity2, String::class.java)

        @Suppress("UNCHECKED_CAST")
        val responseMap1 = objectMapper.readValue(response1.body!!, Map::class.java) as Map<String, Any>
        val responseMap2 = objectMapper.readValue(response2.body!!, Map::class.java) as Map<String, Any>
        val dataMap1 = responseMap1["data"] as? Map<*, *>
        val dataMap2 = responseMap2["data"] as? Map<*, *>
        val chatRoomId1 = dataMap1?.get("id") as? String
        val chatRoomId2 = dataMap2?.get("id") as? String
        assertNotNull(chatRoomId1)
        assertNotNull(chatRoomId2)

        // 사용자가 두 채팅방에 참여
        val joinHeaders = HttpHeaders()
        joinHeaders.set("Authorization", "Bearer $userToken")
        val joinEntity = HttpEntity<Nothing?>(null, joinHeaders)

        val joinResponse1 = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId1/members",
            HttpMethod.POST,
            joinEntity,
            String::class.java
        )
        val joinResponse2 = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId2/members",
            HttpMethod.POST,
            joinEntity,
            String::class.java
        )
        assertEquals(HttpStatus.CREATED, joinResponse1.statusCode)
        assertEquals(HttpStatus.CREATED, joinResponse2.statusCode)

        // when - 참여한 채팅방 목록 조회
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $userToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val response = restTemplate.exchange(
            baseUrl(),
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
        assertTrue(dataList.size >= 2, "참여한 채팅방이 2개 이상이어야 합니다")
        
        // 두 채팅방이 모두 목록에 포함되어 있는지 확인
        val chatRoomIds = dataList.mapNotNull { (it as? Map<*, *>)?.get("id") as? String }
        assertTrue(chatRoomIds.contains(chatRoomId1), "채팅방 A가 목록에 포함되어야 합니다")
        assertTrue(chatRoomIds.contains(chatRoomId2), "채팅방 B가 목록에 포함되어야 합니다")
    }

    @Test
    fun `내가 참여한 채팅방 목록 조회 성공 - 참여한 채팅방이 없을 때`() {
        // given
        val accessToken = loginAndGetToken("nochat@example.com", "password123")

        // when - 참여한 채팅방 목록 조회
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $accessToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val response = restTemplate.exchange(
            baseUrl(),
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
        assertTrue(dataList.isEmpty(), "참여한 채팅방이 없으면 빈 리스트를 반환해야 합니다")
    }

    @Test
    fun `채팅방 수정 성공`() {
        // given
        val accessToken = loginAndGetToken()
        
        // 채팅방 생성
        val createRequest = ChatRoomRequest.Create(name = "원래 이름")

        val createHeaders = HttpHeaders()
        createHeaders.set("Authorization", "Bearer $accessToken")
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(objectMapper.writeValueAsString(createRequest), createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            createEntity,
            String::class.java
        )

        @Suppress("UNCHECKED_CAST")
        val createResponseMap = objectMapper.readValue(createResponse.body!!, Map::class.java) as Map<String, Any>
        val createDataMap = createResponseMap["data"] as? Map<*, *>
        val chatRoomId = createDataMap?.get("id") as? String
        assertNotNull(chatRoomId)

        // when - 채팅방 수정
        val updateRequest = ChatRoomRequest.Update(name = "수정된 이름")

        val updateHeaders = HttpHeaders()
        updateHeaders.set("Authorization", "Bearer $accessToken")
        updateHeaders.contentType = MediaType.APPLICATION_JSON
        val updateEntity = HttpEntity(objectMapper.writeValueAsString(updateRequest), updateHeaders)

        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.PUT,
            updateEntity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(responseMap["success"] as Boolean)
        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        assertEquals("수정된 이름", dataMap["name"])
    }

    @Test
    fun `채팅방 삭제 성공`() {
        // given
        val accessToken = loginAndGetToken()
        
        // 채팅방 생성
        val createRequest = ChatRoomRequest.Create(name = "삭제될 채팅방")

        val createHeaders = HttpHeaders()
        createHeaders.set("Authorization", "Bearer $accessToken")
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(objectMapper.writeValueAsString(createRequest), createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            createEntity,
            String::class.java
        )

        @Suppress("UNCHECKED_CAST")
        val createResponseMap = objectMapper.readValue(createResponse.body!!, Map::class.java) as Map<String, Any>
        val createDataMap = createResponseMap["data"] as? Map<*, *>
        val chatRoomId = createDataMap?.get("id") as? String
        assertNotNull(chatRoomId)

        // when - 채팅방 삭제
        val deleteHeaders = HttpHeaders()
        deleteHeaders.set("Authorization", "Bearer $accessToken")
        val deleteEntity = HttpEntity<Nothing?>(null, deleteHeaders)

        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.DELETE,
            deleteEntity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.OK, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>

        assertTrue(responseMap["success"] as Boolean)
        val dataMap = responseMap["data"] as? Map<*, *>
        assertNotNull(dataMap)
        assertEquals(chatRoomId, dataMap["id"])

        // 삭제 후 조회 시 404 확인
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $accessToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val getResponse = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.GET,
            getEntity,
            String::class.java
        )

        assertEquals(HttpStatus.NOT_FOUND, getResponse.statusCode)
    }

    @Test
    fun `채팅방 수정 실패 - 권한 없음`() {
        // given
        val creatorToken = loginAndGetToken("creator@example.com", "password123")
        val otherUserToken = loginAndGetToken("other@example.com", "password123")
        
        // 생성자가 채팅방 생성
        val createRequest = ChatRoomRequest.Create(name = "원래 이름")

        val createHeaders = HttpHeaders()
        createHeaders.set("Authorization", "Bearer $creatorToken")
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(objectMapper.writeValueAsString(createRequest), createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            createEntity,
            String::class.java
        )

        @Suppress("UNCHECKED_CAST")
        val createResponseMap = objectMapper.readValue(createResponse.body!!, Map::class.java) as Map<String, Any>
        val createDataMap = createResponseMap["data"] as? Map<*, *>
        val chatRoomId = createDataMap?.get("id") as? String
        assertNotNull(chatRoomId)

        // when - 다른 사용자가 수정 시도
        val updateRequest = ChatRoomRequest.Update(name = "수정 시도")

        val updateHeaders = HttpHeaders()
        updateHeaders.set("Authorization", "Bearer $otherUserToken")
        updateHeaders.contentType = MediaType.APPLICATION_JSON
        val updateEntity = HttpEntity(objectMapper.writeValueAsString(updateRequest), updateHeaders)

        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.PUT,
            updateEntity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])
    }

    @Test
    fun `채팅방 삭제 실패 - 권한 없음`() {
        // given
        val creatorToken = loginAndGetToken("creator2@example.com", "password123")
        val otherUserToken = loginAndGetToken("other2@example.com", "password123")
        
        // 생성자가 채팅방 생성
        val createRequest = ChatRoomRequest.Create(name = "삭제될 채팅방")

        val createHeaders = HttpHeaders()
        createHeaders.set("Authorization", "Bearer $creatorToken")
        createHeaders.contentType = MediaType.APPLICATION_JSON
        val createEntity = HttpEntity(objectMapper.writeValueAsString(createRequest), createHeaders)

        val createResponse = restTemplate.exchange(
            baseUrl(),
            HttpMethod.POST,
            createEntity,
            String::class.java
        )

        @Suppress("UNCHECKED_CAST")
        val createResponseMap = objectMapper.readValue(createResponse.body!!, Map::class.java) as Map<String, Any>
        val createDataMap = createResponseMap["data"] as? Map<*, *>
        val chatRoomId = createDataMap?.get("id") as? String
        assertNotNull(chatRoomId)

        // when - 다른 사용자가 삭제 시도
        val deleteHeaders = HttpHeaders()
        deleteHeaders.set("Authorization", "Bearer $otherUserToken")
        val deleteEntity = HttpEntity<Nothing?>(null, deleteHeaders)

        val response = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.DELETE,
            deleteEntity,
            String::class.java
        )

        // then
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode, "응답: ${response.body}")
        assertNotNull(response.body)

        @Suppress("UNCHECKED_CAST")
        val responseMap = objectMapper.readValue(response.body!!, Map::class.java) as Map<String, Any>
        assertTrue(!(responseMap["success"] as Boolean))
        assertNotNull(responseMap["error"])

        // 삭제되지 않았는지 확인 (생성자로 조회)
        val getHeaders = HttpHeaders()
        getHeaders.set("Authorization", "Bearer $creatorToken")
        val getEntity = HttpEntity<Nothing?>(null, getHeaders)

        val getResponse = restTemplate.exchange(
            "${baseUrl()}/$chatRoomId",
            HttpMethod.GET,
            getEntity,
            String::class.java
        )

        assertEquals(HttpStatus.OK, getResponse.statusCode, "채팅방이 삭제되지 않아야 합니다")
    }
}
