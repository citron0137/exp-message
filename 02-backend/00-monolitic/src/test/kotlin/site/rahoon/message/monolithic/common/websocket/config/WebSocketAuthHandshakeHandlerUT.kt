package site.rahoon.message.monolithic.common.websocket.config

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.server.ServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import site.rahoon.message.monolithic.common.websocket.config.auth.WebSocketAuthHandshakeHandler
import java.net.URI

/**
 * WebSocketAuthHandshakeHandler 단위 테스트
 *
 * Handshake에서는 Principal을 설정하지 않고, 쿼리/헤더의 토큰만 세션 attributes에 저장합니다.
 */
class WebSocketAuthHandshakeHandlerUT {
    private val testableHandler = TestableWebSocketAuthHandshakeHandler()

    @Test
    fun `access_token 쿼리가 있으면 세션에 토큰 저장하고 Principal은 null`() {
        val request = mockk<ServerHttpRequest>()
        every { request.uri } returns URI.create("http://localhost/ws?access_token=token-query")
        every { request.headers } returns HttpHeaders()

        val attributes = mutableMapOf<String, Any>()
        val principal = testableHandler.determineUserPublic(request, mockk(relaxed = true), attributes)

        principal.shouldBeNull()
        attributes[WebSocketAuthHandshakeHandler.ATTR_TOKEN] shouldBe "token-query"
    }

    @Test
    fun `Authorization 헤더가 있으면 세션에 토큰 저장하고 Principal은 null`() {
        val request = mockk<ServerHttpRequest>()
        every { request.uri } returns URI.create("http://localhost/ws")
        every { request.headers } returns HttpHeaders().apply { set("Authorization", "Bearer token-header") }

        val attributes = mutableMapOf<String, Any>()
        val principal = testableHandler.determineUserPublic(request, mockk(relaxed = true), attributes)

        principal.shouldBeNull()
        attributes[WebSocketAuthHandshakeHandler.ATTR_TOKEN] shouldBe "Bearer token-header"
    }

    @Test
    fun `쿼리 우선 - access_token과 Authorization 둘 다 있으면 쿼리 값 저장`() {
        val request = mockk<ServerHttpRequest>()
        every { request.uri } returns URI.create("http://localhost/ws?access_token=from-query")
        every { request.headers } returns HttpHeaders().apply { set("Authorization", "Bearer from-header") }

        val attributes = mutableMapOf<String, Any>()
        testableHandler.determineUserPublic(request, mockk(relaxed = true), attributes)

        attributes[WebSocketAuthHandshakeHandler.ATTR_TOKEN] shouldBe "from-query"
    }

    @Test
    fun `토큰이 없으면 Principal null이고 세션에 토큰 없음`() {
        val request = mockk<ServerHttpRequest>()
        every { request.uri } returns URI.create("http://localhost/ws")
        every { request.headers } returns HttpHeaders()

        val attributes = mutableMapOf<String, Any>()
        val principal = testableHandler.determineUserPublic(request, mockk(relaxed = true), attributes)

        principal.shouldBeNull()
        attributes.containsKey(WebSocketAuthHandshakeHandler.ATTR_TOKEN) shouldBe false
    }

    private class TestableWebSocketAuthHandshakeHandler : WebSocketAuthHandshakeHandler() {
        fun determineUserPublic(
            request: ServerHttpRequest,
            wsHandler: WebSocketHandler,
            attributes: MutableMap<String, Any>,
        ): java.security.Principal? = super.determineUser(request, wsHandler, attributes)
    }
}
