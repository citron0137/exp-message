package site.rahoon.message.monolithic.common.websocket.exception

import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.domain.CommonError
import site.rahoon.message.monolithic.common.domain.DomainException
import site.rahoon.message.monolithic.core.shared.error.CoreException
import java.time.ZonedDateTime

/**
 * 다양한 입력으로 [WebSocketExceptionBody]를 만든다.
 *
 * - [DomainException](또는 cause 체인 내 DomainException): code, message, details 추출.
 * - 그 외: [CommonError.SERVER_ERROR]로 [DomainException]을 만들어 body 반환.
 */
@Component
class WebSocketExceptionBodyBuilder {
    /**
     * Throwable에서 DomainException을 찾아 [WebSocketExceptionBody]로 변환.
     * cause 체인 내 DomainException이 있으면 해당 값 사용.
     * DomainException이 아니면 [CommonError.SERVER_ERROR]로 body를 만든다.
     * [websocketSessionId], [receiptId], [requestDestination]이 있으면 body에 포함한다.
     */
    fun build(
        throwable: Throwable,
        websocketSessionId: String? = null,
        receiptId: String? = null,
        requestDestination: String? = null,
    ): WebSocketExceptionBody {
        val coreException = resolveCoreException(throwable)
        if (coreException != null) {
            return fromCoreException(coreException, websocketSessionId, receiptId, requestDestination)
        }
        val domainException =
            resolveDomainException(throwable)
                ?: DomainException(
                    CommonError.SERVER_ERROR,
                    mapOf("reason" to (throwable.message ?: "Unknown error")),
                    throwable,
                )
        return fromDomainException(domainException, websocketSessionId, receiptId, requestDestination)
    }

    /**
     * [DomainException]을 그대로 [WebSocketExceptionBody]로 변환.
     */
    fun fromDomainException(
        domainException: DomainException,
        websocketSessionId: String? = null,
        receiptId: String? = null,
        requestDestination: String? = null,
    ): WebSocketExceptionBody =
        WebSocketExceptionBody(
            code = domainException.error.code,
            message = domainException.error.message,
            details = domainException.details,
            occurredAt = ZonedDateTime.now(),
            websocketSessionId = websocketSessionId,
            receiptId = receiptId,
            requestDestination = requestDestination,
        )

    /**
     * Converts a core exception to a WebSocket exception body.
     */
    fun fromCoreException(
        coreException: CoreException,
        websocketSessionId: String? = null,
        receiptId: String? = null,
        requestDestination: String? = null,
    ): WebSocketExceptionBody =
        WebSocketExceptionBody(
            code = coreException.error.code,
            message = coreException.error.userMessage,
            details = coreException.details.filterValues { it != null }.mapValues { it.value as Any },
            occurredAt = ZonedDateTime.now(),
            websocketSessionId = websocketSessionId,
            receiptId = receiptId,
            requestDestination = requestDestination,
        )

    private fun resolveDomainException(throwable: Throwable): DomainException? {
        var t: Throwable? = throwable
        while (t != null) {
            if (t is DomainException) return t
            t = t.cause
        }
        return null
    }

    private fun resolveCoreException(throwable: Throwable): CoreException? {
        var t: Throwable? = throwable
        while (t != null) {
            if (t is CoreException) return t
            t = t.cause
        }
        return null
    }
}
