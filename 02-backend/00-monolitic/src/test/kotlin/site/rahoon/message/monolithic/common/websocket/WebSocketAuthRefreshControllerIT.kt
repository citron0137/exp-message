package site.rahoon.message.monolithic.common.websocket

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
import org.springframework.messaging.converter.MappingJackson2MessageConverter
import org.springframework.messaging.simp.stomp.StompFrameHandler
import org.springframework.messaging.simp.stomp.StompHeaders
import org.springframework.messaging.simp.stomp.StompSession
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.messaging.WebSocketStompClient
import org.springframework.web.socket.sockjs.client.SockJsClient
import org.springframework.web.socket.sockjs.client.WebSocketTransport
import site.rahoon.message.monolithic.authtoken.application.AuthApplicationITUtils
import site.rahoon.message.monolithic.authtoken.application.AuthTokenApplicationService
import site.rahoon.message.monolithic.chatroom.controller.ChatRoomRequest
import site.rahoon.message.monolithic.chatroom.controller.ChatRoomResponse
import site.rahoon.message.monolithic.common.test.IntegrationTestBase
import site.rahoon.message.monolithic.common.test.assertSuccess
import site.rahoon.message.monolithic.message.controller.MessageRequest
import site.rahoon.message.monolithic.message.controller.MessageResponse
import site.rahoon.message.monolithic.message.websocket.MessageWsSend
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * WebSocket /app/auth/refresh 엔드포인트 통합 테스트.
 *
 * - 성공: 연결 후 새 토큰으로 SEND /app/auth/refresh → 세션 갱신, 이후 구독·수신 정상
 * - 실패(토큰 없음/잘못된 토큰): 서버가 예외 없이 ERROR 응답 처리하는지 스모크 검증.
 *   ERROR payload 형식(code·message)은 [WebSocketExceptionStompSubProtocolErrorHandlerUT]에서 검증.
 */
class WebSocketAuthRefreshControllerIT(
    private val restTemplate: TestRestTemplate,
    private val objectMapper: ObjectMapper,
    private val authApplicationITUtils: AuthApplicationITUtils,
    private val authTokenApplicationService: AuthTokenApplicationService,
    @LocalServerPort private var port: Int = 0,
) : IntegrationTestBase() {
    @Test
    fun `auth refresh 성공 - 새 토큰으로 SEND 후 구독 수신 정상`() {
        // given: 로그인, 채팅방 생성 → 첫 액세스 토큰으로 WebSocket 연결
        val authResult = authApplicationITUtils.signUpAndLogin()
        val chatRoomId = createChatRoom(authResult.accessToken)
        val wsUrl = "http://localhost:$port/ws?access_token=${authResult.accessToken}"
        val stompClient = createStompClient()
        val session: StompSession =
            stompClient
                .connect(wsUrl, object : StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS)

        // REST refresh로 새 액세스 토큰 발급
        val refreshed = authTokenApplicationService.refresh(authResult.refreshToken)
        val newAccessToken = refreshed.accessToken.token

        // when: SEND /app/auth/refresh (헤더에 새 토큰)
        val headers = StompHeaders().apply {
            destination = "/app/auth/refresh"
            add("Authorization", "Bearer $newAccessToken")
        }
        session.send(headers, "")

        Thread.sleep(500)

        // then: 갱신된 세션으로 본인 메시지 토픽 구독 후 메시지 수신 가능
        val receives = ArrayBlockingQueue<MessageWsSend.Detail>(1)
        session.subscribe(
            "/topic/user/${authResult.userId}/messages",
            object : StompFrameHandler {
                override fun getPayloadType(headers: StompHeaders): java.lang.reflect.Type = MessageWsSend.Detail::class.java

                override fun handleFrame(
                    headers: StompHeaders,
                    payload: Any?,
                ) {
                    if (payload is MessageWsSend.Detail) receives.offer(payload)
                }
            },
        )
        Thread.sleep(1000)

        val content = "auth-refresh-it 메시지"
        val createRequest = MessageRequest.Create(chatRoomId = chatRoomId, content = content)
        val entity =
            HttpEntity(
                objectMapper.writeValueAsString(createRequest),
                HttpHeaders().apply {
                    set("Authorization", "Bearer $newAccessToken")
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

        val received = receives.poll(5, TimeUnit.SECONDS).shouldNotBeNull()
        received.content shouldBe content
    }

    @Test
    fun `auth refresh 실패 - 토큰 없이 SEND 시 서버가 예외 없이 처리`() {
        val authResult = authApplicationITUtils.signUpAndLogin()
        val wsUrl = "http://localhost:$port/ws?access_token=${authResult.accessToken}"
        val stompClient = createStompClient()
        val session: StompSession =
            stompClient.connect(wsUrl, object : StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS)

        val sendHeaders = StompHeaders().apply {
            destination = "/app/auth/refresh"
            // Authorization 헤더 없음
        }
        session.send(sendHeaders, "")

        // 서버가 DomainException → WebSocketExceptionStompSubProtocolErrorHandler로 ERROR 전송 후 정상 처리. 클라이언트는 연결 끊김 가능.
        // ERROR payload 형식은 WebSocketExceptionStompSubProtocolErrorHandlerUT에서 검증.
    }

    @Test
    fun `auth refresh 실패 - 잘못된 토큰으로 SEND 시 서버가 예외 없이 처리`() {
        val authResult = authApplicationITUtils.signUpAndLogin()
        val wsUrl = "http://localhost:$port/ws?access_token=${authResult.accessToken}"
        val stompClient = createStompClient()
        val session: StompSession =
            stompClient.connect(wsUrl, object : StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS)

        val sendHeaders = StompHeaders().apply {
            destination = "/app/auth/refresh"
            add("Authorization", "Bearer invalid-token")
        }
        session.send(sendHeaders, "")

        // 서버가 DomainException → WebSocketExceptionStompSubProtocolErrorHandler로 ERROR 전송 후 정상 처리.
        // ERROR payload 형식은 WebSocketExceptionStompSubProtocolErrorHandlerUT에서 검증.
    }

    private fun createStompClient(): WebSocketStompClient {
        val transports = listOf(WebSocketTransport(StandardWebSocketClient()))
        val sockJsClient = SockJsClient(transports)
        val stompClient = WebSocketStompClient(sockJsClient)
        val converter = MappingJackson2MessageConverter()
        converter.objectMapper = objectMapper
        stompClient.setMessageConverter(converter)
        return stompClient
    }

    private fun createChatRoom(accessToken: String): String {
        val request = ChatRoomRequest.Create(name = "auth-refresh-it방")
        val entity =
            HttpEntity(
                objectMapper.writeValueAsString(request),
                HttpHeaders().apply {
                    set("Authorization", "Bearer $accessToken")
                    contentType = MediaType.APPLICATION_JSON
                },
            )
        val res =
            restTemplate
                .exchange(
                    "http://localhost:$port/chat-rooms",
                    HttpMethod.POST,
                    entity,
                    String::class.java,
                ).assertSuccess<ChatRoomResponse.Create>(objectMapper, org.springframework.http.HttpStatus.CREATED) { }
        return res.id
    }
}
