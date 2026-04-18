package site.rahoon.message.monolithic.presentation.websocket.admin

import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal

object AdminWebSocketSession {
    const val ATTR_PRINCIPAL = "admin.core.principal"

    /**
     * Stores the authenticated core admin principal in the STOMP session.
     */
    fun store(
        accessor: StompHeaderAccessor,
        principal: AuthenticatedPrincipal,
    ) {
        (accessor.sessionAttributes as? MutableMap<String, Any>)?.set(ATTR_PRINCIPAL, principal)
    }

    /**
     * Returns the authenticated core admin principal from the STOMP session.
     */
    fun require(accessor: StompHeaderAccessor): AuthenticatedPrincipal =
        accessor.sessionAttributes?.get(ATTR_PRINCIPAL) as? AuthenticatedPrincipal
            ?: throw IllegalStateException("Admin WebSocket principal is missing")
}
