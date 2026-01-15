package site.rahoon.message.__monolitic.test.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.Optional

/**
 * TestEntity를 위한 Spring Data JPA Repository
 * Soft Delete된 엔티티는 Hibernate Filter로 자동으로 제외됩니다.
 * 
 * Note: Spring Data JPA의 기본 메서드(findById)와 메서드명 기반 쿼리는 
 * Hibernate Filter를 사용하지 않을 수 있으므로, @Query를 사용하여 명시적으로 조건을 추가합니다.
 */
interface TestJpaRepository : JpaRepository<TestEntity, String> {
    fun findByName(name: String): TestEntity?
    
    @Query("""SELECT t FROM TestEntity t WHERE t.description LIKE %:pattern% """)
    fun findWithDescriptionLike(@Param("pattern") pattern: String): List<TestEntity>

    /**
     * Soft Delete를 수행합니다.
     * 원자적 연산으로 deleted_at을 업데이트합니다.
     */
    @Modifying(clearAutomatically = true) // 쿼리 실행 후 영속성 컨텍스트를 비워 정합성 유지
    @Transactional
    @Query("UPDATE TestEntity e SET e.deletedAt = :deletedAt WHERE e.id = :id AND e.deletedAt IS NULL")
    fun softDeleteById(@Param("id") id: String, @Param("deletedAt") deletedAt: LocalDateTime): Int
}
