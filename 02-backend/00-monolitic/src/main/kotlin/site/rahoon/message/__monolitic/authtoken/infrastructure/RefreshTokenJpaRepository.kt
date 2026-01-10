package site.rahoon.message.__monolitic.authtoken.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface RefreshTokenJpaRepository : JpaRepository<RefreshTokenEntity, String> {
    @Transactional
    fun deleteAllByUserId(userId: String)
}

