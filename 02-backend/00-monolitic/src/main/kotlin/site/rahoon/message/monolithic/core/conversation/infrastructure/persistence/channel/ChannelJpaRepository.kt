package site.rahoon.message.monolithic.core.conversation.infrastructure.persistence.channel

import org.springframework.data.jpa.repository.JpaRepository

interface ChannelJpaRepository : JpaRepository<ChannelEntity, String>
