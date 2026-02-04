package site.rahoon.message.monolithic.common.websocket.config

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.SimpMessagingTemplate
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionBodyBuilder
import site.rahoon.message.monolithic.common.websocket.exception.WebSocketExceptionController

/**
 * WebSocketExceptionStompSubProtocolErrorHandler 단위 테스트.
 *
 * DomainException 발생 시 ERROR 프레임 payload에 code·message가 담기는지 검증.
 */
class WebSocketExceptionStompSubProtocolErrorHandlerUT {
    private val objectMapper = jacksonObjectMapper()
    private val mockChannel = mockk<MessageChannel>(relaxed = true)
    private val mockMessagingTemplate = mockk<SimpMessagingTemplate>(relaxed = true)
    private val exceptionController = WebSocketExceptionController(objectMapper, mockChannel, mockMessagingTemplate)
    private val exceptionBodyBuilder = WebSocketExceptionBodyBuilder()
    private val handler = WebSocketExceptionStompSubProtocolErrorHandler(exceptionBodyBuilder, exceptionController)

    @Test
    fun `DomainException 발생 시 ERROR payload에 code와 message 담김`() {
        val ex = DomainException(CommonError.UNAUTHORIZED, mapOf("reason" to "Authorization required"))
        val result = handler.handleClientMessageProcessingError(null, ex)

        result.shouldNotBeNull()
        val payload = result.payload
        payload.shouldNotBeNull()
        val map: Map<String, Any> = objectMapper.readValue(String(payload))
        map["code"] shouldBe "COMMON_005"
        map["message"] shouldBe "Unauthorized"
    }

    @Test
    fun `cause 체인 내 DomainException이면 ERROR payload에 code와 message 담김`() {
        val domainEx = DomainException(CommonError.UNAUTHORIZED, mapOf())
        val ex = RuntimeException("wrapper", domainEx)
        val result = handler.handleClientMessageProcessingError(null, ex)

        result.shouldNotBeNull()
        val map: Map<String, Any> = objectMapper.readValue(String(result.payload))
        map["code"] shouldBe "COMMON_005"
        map["message"] shouldBe "Unauthorized"
    }
}
