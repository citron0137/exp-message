package site.rahoon.message.__monolitic.common.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.domain.SoftDeleteContext
import site.rahoon.message.__monolitic.common.global.TransactionalRepository

@TransactionalRepository
abstract class JpaRepositoryAdapterBase<Domain, Entity : JpaEntityBase, ID : Any> {

    abstract val jpaRepository: JpaRepository<Entity, ID>
    abstract fun toDomain(entity:Entity): Domain
    abstract fun fromDomain(domain: Domain): Entity

    fun findById(id: ID): Domain? {
        return jpaRepository.findById(id).orElse(null)
            ?.takeIf { SoftDeleteContext.isDisabled() || it.deletedAt == null }
            ?.let { toDomain(it) }
    }

    fun findAll(): List<Domain> {
        return jpaRepository.findAll().map { toDomain(it) }
    }

    @Transactional
    open fun save(domain: Domain):Domain{
        return jpaRepository.save(fromDomain(domain)).let { toDomain(it) }
    }


}

