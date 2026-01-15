package site.rahoon.message.__monolitic.common.infrastructure

import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import jakarta.persistence.metamodel.SingularAttribute
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class JpaSoftDeleteRepository(
    private val entityManager: EntityManager
) {
    /**
     * 원자적 연산을 위해 단일 UPDATE 쿼리로 Soft Delete를 수행합니다.
     * JPA Metamodel을 사용하여 엔티티 이름과 ID 속성 이름을 자동으로 추출합니다.
     * 
     * @param entityClass 삭제할 엔티티의 클래스
     * @param id 삭제할 엔티티의 ID
     * @return 업데이트된 행의 수
     */
    fun <T : JpaEntityBase> softDeleteById(entityClass: Class<T>, id: Any): Int {
        val metamodel = entityManager.metamodel
        val entityType = metamodel.entity(entityClass)
        val entityName = entityType.name
        
        // ID 속성 찾기
        val idAttribute = entityType.attributes
            .filterIsInstance<SingularAttribute<*, *>>()
            .firstOrNull { it.isId }
            ?: throw IllegalStateException("Entity ${entityClass.simpleName} does not have an ID attribute")
        val idPropertyName = idAttribute.name
        
        val now = java.time.LocalDateTime.now()

        val query: Query = entityManager.createQuery(
            "UPDATE $entityName e SET e.deletedAt = :deletedAt WHERE e.$idPropertyName = :id AND e.deletedAt IS NULL"
        )
        query.setParameter("deletedAt", now)
        query.setParameter("id", id)
        return query.executeUpdate()
    }
}
