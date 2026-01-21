package site.rahoon.message.monolithic.message.sse

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
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
 * SSE 구독 후 POST /messages 시 /sse/chat-rooms/{chatRoomId}/messages 로
 * 브로드캐스트되는지 통합 테스트.
 */
class MessageSseIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    @Test
    fun `POST messages 후 SSE 구독자에게 MessageResponse Detail 브로드캐스트`() {
        // given: 로그인, 채팅방 생성
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken)

        // SSE 구독
        val receives = ArrayBlockingQueue<MessageResponse.Detail>(1)
        val errors = ArrayBlockingQueue<Throwable>(1)
        val webClient = WebClient.builder().baseUrl("http://localhost:$port").build()

        @Suppress("UNCHECKED_CAST")
        val sseFlux: Flux<ServerSentEvent<Any>> =
            webClient
                .get()
                .uri("/sse/chat-rooms/$chatRoomId/messages?timeout=10000") // 테스트용 10초 타임아웃
                .header("Authorization", "Bearer ${authResult.accessToken}")
                .retrieve()
                .bodyToFlux(ServerSentEvent::class.java) as Flux<ServerSentEvent<Any>>

        // 백그라운드에서 SSE 수신
        val subscription =
            sseFlux.subscribe(
                { event ->
                    println("SSE event received: name=${event.event()}, data=${event.data()}")
                    // connected 이벤트는 무시
                    if (event.event() == "connected") {
                        return@subscribe
                    }
                    if (event.event() == "message") {
                        val data = event.data()
                        if (data != null) {
                            // LinkedHashMap 또는 String을 JSON으로 변환
                            val jsonString = when (data) {
                                is String -> data
                                else -> objectMapper.writeValueAsString(data)
                            }
                            val message = objectMapper.readValue(jsonString, MessageResponse.Detail::class.java)
                            receives.offer(message)
                        }
                    }
                },
                { error ->
                    println("SSE error: ${error.javaClass.simpleName} - ${error.message}")
                    error.printStackTrace()
                    errors.offer(error)
                },
            )

        // SSE 연결 대기
        Thread.sleep(500)

        // when: 메시지 전송
        val content = "SSE IT 메시지"
        val createRequest = MessageRequest.Create(chatRoomId = chatRoomId, content = content)
        val entity =
            HttpEntity(
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

        // then: SSE 구독자에게 수신
        val received = receives.poll(5, TimeUnit.SECONDS)

        // 에러가 있으면 먼저 확인
        if (received == null && errors.isNotEmpty()) {
            val error = errors.poll()
            throw AssertionError("SSE subscription error: ${error?.message}", error)
        }

        received.shouldNotBeNull()
        received.content shouldBe content
        received.chatRoomId shouldBe chatRoomId

        // cleanup: 구독 해제 후 연결 정리 대기
        subscription.dispose()
        Thread.sleep(100) // 서버가 정상적으로 연결을 닫을 시간 부여
    }

    private fun createChatRoom(accessToken: String): String {
        val request = ChatRoomRequest.Create(name = "SSE테스트방")
        val entity =
            HttpEntity(
                objectMapper.writeValueAsString(request),
                HttpHeaders().apply {
                    set("Authorization", "Bearer $accessToken")
                    contentType = MediaType.APPLICATION_JSON
                },
            )
        val res =
            restTemplate.exchange(
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
