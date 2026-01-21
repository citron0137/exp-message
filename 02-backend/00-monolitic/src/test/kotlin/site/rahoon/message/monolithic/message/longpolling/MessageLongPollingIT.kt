package site.rahoon.message.monolithic.message.longpolling

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import site.rahoon.message.monolithic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.monolithic.chatroom.controller.ChatRoomRequest
import site.rahoon.message.monolithic.chatroom.controller.ChatRoomResponse
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import site.rahoon.message.monolithic.common.test.assertSuccess
import site.rahoon.message.monolithic.message.controller.MessageRequest
import site.rahoon.message.monolithic.message.controller.MessageResponse
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Long Polling 요청 후 POST /messages 시 대기 중인 요청에 응답이 오는지 통합 테스트.
 */
class MessageLongPollingIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    @Test
    fun `POST messages 후 Long Polling 대기 중인 요청에 MessageResponse Detail 응답`() {
        // given: 로그인, 채팅방 생성
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken)

        // Long Polling 요청 (비동기)
        val receives = ArrayBlockingQueue<List<MessageResponse.Detail>>(1)
        val errors = ArrayBlockingQueue<Throwable>(1)

        val pollingThread = Thread {
            try {
                val headers = HttpHeaders().apply {
                    set("Authorization", "Bearer ${authResult.accessToken}")
                }
                val entity = HttpEntity<String>(headers)

                val response = restTemplate.exchange(
                    "http://localhost:$port/long-polling/chat-rooms/$chatRoomId/messages?timeout=10000", // 테스트용 10초 타임아웃
                    HttpMethod.GET,
                    entity,
                    object : ParameterizedTypeReference<List<MessageResponse.Detail>>() {},
                )

                if (response.statusCode.is2xxSuccessful) {
                    val messages = response.body
                    println("Long polling response received: $messages")
                    messages?.let { receives.offer(it) }
                }
            } catch (e: Exception) {
                println("Long polling error: ${e.javaClass.simpleName} - ${e.message}")
                e.printStackTrace()
                errors.offer(e)
            }
        }
        pollingThread.start()

        // Long Polling 요청이 서버에 도달할 때까지 대기
        Thread.sleep(500)

        // when: 메시지 전송
        val content = "Long Polling IT 메시지"
        val createRequest = MessageRequest.Create(chatRoomId = chatRoomId, content = content)
        val entity = HttpEntity(
            objectMapper.writeValueAsString(createRequest),
            HttpHeaders().apply {
                set("Authorization", "Bearer ${authResult.accessToken}")
                contentType = MediaType.APPLICATION_JSON
            },
        )
        restTemplate
            .exchange(
                "http://localhost:$port/messages",
                HttpMethod.POST,
                entity,
                String::class.java,
            ).assertSuccess<MessageResponse.Create>(objectMapper, org.springframework.http.HttpStatus.CREATED) { data ->
                data.content shouldBe content
            }

        // @Async 이벤트 처리 대기
        Thread.sleep(100)

        // then: Long Polling 요청에 응답
        val received = receives.poll(5, TimeUnit.SECONDS)

        // 에러가 있으면 먼저 확인
        if (received == null && errors.isNotEmpty()) {
            val error = errors.poll()
            throw AssertionError("Long polling error: ${error?.message}", error)
        }

        received.shouldNotBeNull()
        received shouldHaveSize 1
        received[0].content shouldBe content
        received[0].chatRoomId shouldBe chatRoomId

        // cleanup: 스레드 종료 대기
        pollingThread.join(1000)
    }

    @Test
    fun `Long Polling 타임아웃 시 빈 리스트 응답`() {
        // given: 로그인, 채팅방 생성
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken)

        // when: Long Polling 요청 (메시지 전송 없음)
        val headers = HttpHeaders().apply {
            set("Authorization", "Bearer ${authResult.accessToken}")
        }
        val entity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            "http://localhost:$port/long-polling/chat-rooms/$chatRoomId/messages?timeout=1000", // 1초 타임아웃
            HttpMethod.GET,
            entity,
            object : ParameterizedTypeReference<List<MessageResponse.Detail>>() {},
        )

        // then: 빈 리스트 응답
        response.statusCode.is2xxSuccessful shouldBe true
        val messages = response.body
        messages.shouldNotBeNull()
        messages shouldHaveSize 0
    }

    private fun createChatRoom(accessToken: String): String {
        val request = ChatRoomRequest.Create(name = "LongPolling테스트방")
        val entity = HttpEntity(
            objectMapper.writeValueAsString(request),
            HttpHeaders().apply {
                set("Authorization", "Bearer $accessToken")
                contentType = MediaType.APPLICATION_JSON
            },
        )
        val res = restTemplate.exchange(
            "http://localhost:$port/chat-rooms",
            HttpMethod.POST,
            entity,
            String::class.java,
        )
        return res
            .assertSuccess<ChatRoomResponse.Create>(objectMapper, org.springframework.http.HttpStatus.CREATED) { }
            .id
    }
}
