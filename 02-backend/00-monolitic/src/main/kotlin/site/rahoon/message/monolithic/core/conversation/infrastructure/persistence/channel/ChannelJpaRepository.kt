package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.channel

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository("conversationChannelJpaRepository")
interface ChannelJpaRepository : JpaRepository<ChannelEntity, String>
