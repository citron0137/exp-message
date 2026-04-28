package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.visitor

import org.springframework.data.jpa.repository.JpaRepository

interface VisitorJpaRepository : JpaRepository<VisitorEntity, String>
