package site.rahoon.message.__monolitic.test.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.global.TransactionalRepository
import site.rahoon.message.__monolitic.common.infrastructure.JpaRepositoryAdapterBase
import site.rahoon.message.__monolitic.test.domain.TestDomain
import site.rahoon.message.__monolitic.test.domain.TestRepository
import java.time.LocalDateTime

@TransactionalRepository
class TestRepositoryImpl(
    private val testJpaRepository: TestJpaRepository,
) : TestRepository,
    JpaRepositoryAdapterBase<TestDomain, TestEntity, String>()
{
    override val jpaRepository: JpaRepository<TestEntity, String>
        get() = testJpaRepository

    override fun toDomain(entity: TestEntity): TestDomain {
        return TestDomain(
            id = entity.id,
            name = entity.name,
            description = entity.description,
            createdAt = entity.createdAt,
            deletedAt = entity.deletedAt,
        )
    }

    override fun fromDomain(domain: TestDomain): TestEntity {
        return TestEntity(
            id = domain.id,
            name = domain.name,
            description = domain.description,
            createdAt = domain.createdAt,
        )
    }

    override fun findByName(name: String): TestDomain? {
        return testJpaRepository.findByName(name)?.let { toDomain(it) }
    }

    override fun findWithDescriptionLike(descriptionPattern: String): List<TestDomain> {
        return testJpaRepository.findWithDescriptionLike(descriptionPattern).map { toDomain(it) }
    }

    override fun findWithSelfJoin(): List<TestDomain> {
        return testJpaRepository.findWithSelfJoin().map { toDomain(it) }
    }

    @Transactional
    override fun deleteById(id:String){
        testJpaRepository.softDeleteById(id, LocalDateTime.now())
    }


    fun create(id: String, name: String, description: String? = null): TestDomain{
        return save(TestDomain(id,name, description))
    }
}
