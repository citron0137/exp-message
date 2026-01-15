package site.rahoon.message.__monolitic.test.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * TestEntity를 위한 Spring Data JPA Repository
 * Soft Delete된 엔티티는 Hibernate Filter로 자동으로 제외됩니다.
 * 
 * Note: Spring Data JPA의 기본 메서드(findById)와 메서드명 기반 쿼리는 
 * Hibernate Filter를 사용하지 않을 수 있으므로, @Query를 사용하여 명시적으로 조건을 추가합니다.
 */
interface RawTestJpaRepository : JpaRepository<RawTestEntity, String> {
    fun findByName(@Param("name") name: String): RawTestEntity?
}
