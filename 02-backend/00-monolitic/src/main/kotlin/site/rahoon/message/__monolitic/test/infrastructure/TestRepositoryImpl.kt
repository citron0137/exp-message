package site.rahoon.message.__monolitic.test.infrastructure

import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.global.TransactionalRepository
import site.rahoon.message.__monolitic.common.infrastructure.JpaSoftDeleteRepository
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
    private val jpaRepository: TestJpaRepository,
    private val softDeleteRepository: JpaSoftDeleteRepository
) : TestRepository {
    
    /** TestEntity를 저장합니다.*/
    @Transactional
    override fun save(entity: TestEntity): TestEntity {
        return jpaRepository.save(entity)
    }

    /**
     * ID로 TestEntity를 조회합니다.
     * Soft Delete된 엔티티는 조회되지 않습니다
     */
    override fun findById(id: String): TestEntity? {
        return jpaRepository.findById(id).orElse(null)?.takeIf { it.deletedAt == null }
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
     * 트랜잭션은 JpaSoftDeleteRepositoryImpl의 softDeleteById 메서드에서 자동으로 관리됩니다.
     */
    @Transactional
    override fun delete(id: String) {
        softDeleteRepository.softDeleteById(TestEntity::class.java, id)
    }

    /**
     * 설명(description)에 특정 패턴이 포함된 TestEntity를 조회합니다.
     * 조인 쿼리를 사용하여 Soft Delete된 엔티티는 제외됩니다.
     */
    override fun findWithDescriptionLike(descriptionPattern: String): List<TestEntity> {
        return jpaRepository.findWithDescriptionLike(descriptionPattern)
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
