package site.rahoon.message.monolithic.core.conversation.application.port

import site.rahoon.message.monolithic.core.conversation.domain.Visitor

interface VisitorRepository {
    /**
     * Saves a visitor.
     */
    fun save(visitor: Visitor): Visitor

    /**
     * Finds a visitor by identifier.
     */
    fun findById(id: String): Visitor?
}
