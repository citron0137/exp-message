package site.rahoon.message.__monolitic.test.infrastructure

import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.global.TransactionalRepository
import site.rahoon.message.__monolitic.common.domain.SoftDeleteContext
import site.rahoon.message.__monolitic.test.domain.TestRepository
import java.time.LocalDateTime

/**
 * TestEntity를 위한 Repository 구현체
 * Soft Delete 기능을 테스트하기 위한 간단한 구현입니다.
 * 
 * Soft Delete는 TestJpaRepository의 @Query에서 명시적으로 처리됩니다.
 */
@TransactionalRepository
class TestRepositoryImpl(
    private val jpaRepository: TestJpaRepository
) : TestRepository {
    
    /** TestEntity를 저장합니다.*/
    @Transactional
    override fun save(entity: TestEntity): TestEntity {
        return jpaRepository.save(entity)
    }

    /**
     * ID로 TestEntity를 조회합니다.
     * Soft Delete된 엔티티는 조회되지 않습니다.
     * 필터가 활성화되어 있으면 필터가 자동으로 처리하고,
     * 필터가 비활성화되어 있으면 수동으로 deletedAt을 체크합니다.
     */
    override fun findById(id: String): TestEntity? {
        return jpaRepository.findById(id).orElse(null)
            ?.takeIf { SoftDeleteContext.isDisabled() || it.deletedAt == null }
    }

    /**
     * 이름으로 TestEntity를 조회합니다.
     * Soft Delete된 엔티티는 조회되지 않습니다
     */
    override fun findByName(name: String): TestEntity? {
        return jpaRepository.findByName(name)
    }

    /**
     * 모든 TestEntity를 조회합니다.
     * Soft Delete된 엔티티는 조회되지 않습니다 (@Query에서 처리).
     */
    override fun findAll(): List<TestEntity> {
        return jpaRepository.findAll()
    }

    /**
     * Soft Delete를 수행합니다.
     * 원자적 연산으로 처리됩니다.
     */
    @Transactional
    override fun delete(id: String) {
        jpaRepository.softDeleteById(id, LocalDateTime.now())
    }

    /**
     * 설명(description)에 특정 패턴이 포함된 TestEntity를 조회합니다.
     * 조인 쿼리를 사용하여 Soft Delete된 엔티티는 제외됩니다.
     */
    override fun findWithDescriptionLike(descriptionPattern: String): List<TestEntity> {
        return jpaRepository.findWithDescriptionLike(descriptionPattern)
    }

    /**
     * Self-join 쿼리로 같은 이름을 가진 엔티티들을 조회합니다.
     * 조인된 테이블에도 Filter가 적용되는지 확인하기 위한 테스트용 메서드입니다.
     */
    override fun findWithSelfJoin(): List<TestEntity> {
        return jpaRepository.findWithSelfJoin()
    }

    /**
     * 새로운 TestEntity를 생성합니다.
     */
    @Transactional
    fun create(id: String, name: String, description: String? = null): TestEntity {
        return TestEntity(
            id = id,
            name = name,
            description = description,
            createdAt = LocalDateTime.now()
        )
    }
}
