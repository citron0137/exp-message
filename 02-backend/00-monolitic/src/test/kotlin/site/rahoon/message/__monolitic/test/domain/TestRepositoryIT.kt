package site.rahoon.message.__monolitic.test.domain

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.Session
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import site.rahoon.message.__monolitic.common.global.Tx
import site.rahoon.message.__monolitic.common.test.IntegrationTestBase
import site.rahoon.message.__monolitic.test.infrastructure.RawTestJpaRepository
import site.rahoon.message.__monolitic.test.infrastructure.TestEntity
import site.rahoon.message.__monolitic.test.infrastructure.TestJpaRepository
import site.rahoon.message.__monolitic.test.infrastructure.TestRepositoryImpl
import java.time.LocalDateTime
import java.util.UUID

/**
 * TestRepository 통합 테스트
 * 실제 MySQL(Testcontainers)을 사용하여 Soft Delete 기능을 검증합니다.
 */
class TestRepositoryIT : IntegrationTestBase() {

    @Autowired
    private lateinit var testRepository: TestRepository

    @Autowired
    private lateinit var testRepositoryImpl: TestRepositoryImpl

    @Autowired
    private lateinit var rawTestJpaRepository: RawTestJpaRepository

    @BeforeEach
    fun setUp() {
        // 테스트 전 데이터 정리 (필요한 경우)
    }

    @Test
    fun `엔티티를 저장하면 성공한다`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity = testRepositoryImpl.create(
            id = id,
            name = "test-entity",
            description = "테스트 엔티티"
        )

        // when
        val saved = testRepository.save(entity)

        // then
        assertNotNull(saved)
        assertEquals(id, saved.id)
        assertEquals("test-entity", saved.name)
        assertEquals("테스트 엔티티", saved.description)
        assertNotNull(saved.createdAt)
    }

    @Test
    fun `ID로 엔티티를 조회하면 성공한다`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity = testRepositoryImpl.create(
            id = id,
            name = "find-by-id-test"
        )
        testRepository.save(entity)

        // when
        val found = testRepository.findById(id)

        // then
        assertNotNull(found)
        assertEquals(id, found?.id)
        assertEquals("find-by-id-test", found?.name)
    }

    @Test
    fun `이름으로 엔티티를 조회하면 성공한다`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity = testRepositoryImpl.create(
            id = id,
            name = "find-by-name-test"
        )
        testRepository.save(entity)

        // when
        val found = testRepository.findByName("find-by-name-test")

        // then
        assertNotNull(found)
        assertEquals(id, found?.id)
        assertEquals("find-by-name-test", found?.name)
    }

    @Test
    fun `전체 엔티티를 조회하면 성공한다`() {
        // given
        val entity1 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "find-all-test-1"
        )
        val entity2 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "find-all-test-2"
        )
        testRepository.save(entity1)
        testRepository.save(entity2)

        // when
        val all = testRepository.findAll()

        // then
        assertTrue(all.size >= 2)
        assertTrue(all.any { it.name == "find-all-test-1" })
        assertTrue(all.any { it.name == "find-all-test-2" })
    }

    @Test
    fun `Soft Delete된 엔티티는 ID로 조회되지 않는다`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity = testRepositoryImpl.create(
            id = id,
            name = "soft-delete-test"
        )
        testRepository.save(entity)

        // when
        testRepository.delete(id)

        // then
        val found = testRepository.findById(id)
        assertNull(found, "Soft Delete된 엔티티는 조회되지 않아야 합니다")
    }

    @Test
    fun `Soft Delete된 엔티티는 이름으로 조회되지 않는다`() {
        // given
        val id = UUID.randomUUID().toString()
        val name = "soft-delete-name-test"
        val entity = testRepositoryImpl.create(
            id = id,
            name = name
        )
        testRepository.save(entity)

        // when
        testRepository.delete(id)

        // then
        val found = testRepository.findByName(name)
        assertNull(found, "Soft Delete된 엔티티는 이름으로 조회되지 않아야 합니다")
    }

    @Test
    fun `Soft Delete된 엔티티는 전체 조회에서 제외된다`() {
        // given
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        val entity1 = testRepositoryImpl.create(
            id = id1,
            name = "soft-delete-all-test-1"
        )
        val entity2 = testRepositoryImpl.create(
            id = id2,
            name = "soft-delete-all-test-2"
        )
        testRepository.save(entity1)
        testRepository.save(entity2)

        // when
        testRepository.delete(id1)
        val all = testRepository.findAll()

        // then
        assertFalse(all.any { it.id == id1 }, "Soft Delete된 엔티티는 전체 조회에서 제외되어야 합니다")
        assertTrue(all.any { it.id == id2 }, "삭제되지 않은 엔티티는 조회되어야 합니다")
    }

    @Test
    fun `존재하지 않는 ID로 조회하면 null을 반환한다`() {
        // when
        val found = testRepository.findById("non-existent-id")

        // then
        assertNull(found)
    }

    @Test
    fun `존재하지 않는 이름으로 조회하면 null을 반환한다`() {
        // when
        val found = testRepository.findByName("non-existent-name")

        // then
        assertNull(found)
    }

    @Test
    fun `여러 엔티티를 저장한 후 전체 조회하면 모두 조회된다`() {
        // given
        val entities = (1..5).map { i ->
            testRepositoryImpl.create(
                id = UUID.randomUUID().toString(),
                name = "batch-test-$i",
                description = "배치 테스트 $i"
            )
        }

        // when
        entities.forEach { testRepository.save(it) }
        val all = testRepository.findAll()

        // then
        assertTrue(all.size >= 5)
        entities.forEach { entity ->
            assertTrue(all.any { it.id == entity.id }, "저장한 엔티티 ${entity.id}가 조회되어야 합니다")
        }
    }

    @Test
    fun `Soft Delete된 엔티티와 같은 ID로 저장 시도하면 Primary Key 제약 위반 예외가 발생한다`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity1 = testRepositoryImpl.create(
            id = id,
            name = "recreate-test"
        )
        testRepository.save(entity1)
        testRepository.delete(id)

        // when & then - 같은 ID로 다시 저장 시도하면 Primary Key 제약 위반 예외 발생
        val entity2 = testRepositoryImpl.create(
            id = id,
            name = "recreate-test-2"
        )
        // Soft Delete된 레코드는 물리적으로 존재하므로 같은 ID로 저장할 수 없음
        assertThrows<Exception> {
            testRepository.save(entity2)
        }
    }

    @Test
    fun `설명 패턴으로 검색하면 일치하는 엔티티만 조회된다`() {
        // given
        val entity1 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "join-test-1",
            description = "테스트 설명입니다"
        )
        val entity2 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "join-test-2",
            description = "다른 설명입니다"
        )
        val entity3 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "join-test-3",
            description = null
        )
        testRepository.save(entity1)
        testRepository.save(entity2)
        testRepository.save(entity3)

        // when
        val found = testRepository.findWithDescriptionLike("테스트")

        // then
        assertTrue(found.size >= 1)
        assertTrue(found.any { it.id == entity1.id }, "패턴이 일치하는 엔티티가 조회되어야 합니다")
        assertFalse(found.any { it.id == entity2.id }, "패턴이 일치하지 않는 엔티티는 조회되지 않아야 합니다")
        assertFalse(found.any { it.id == entity3.id }, "description이 null인 엔티티는 조회되지 않아야 합니다")
    }

    @Test
    fun `설명 패턴으로 검색할 때 Soft Delete된 엔티티는 제외된다`() {
        // given
        val entity1 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "join-soft-delete-test-1",
            description = "삭제될 설명"
        )
        val entity2 = testRepositoryImpl.create(
            id = UUID.randomUUID().toString(),
            name = "join-soft-delete-test-2",
            description = "유지될 설명"
        )
        testRepository.save(entity1)
        testRepository.save(entity2)

        // when
        testRepository.delete(entity1.id)
        val found = testRepository.findWithDescriptionLike("설명")

        // then
        assertTrue(found.any { it.id == entity2.id }, "삭제되지 않은 엔티티는 조회되어야 합니다")
        assertFalse(found.any { it.id == entity1.id }, "Soft Delete된 엔티티는 조회되지 않아야 합니다")
    }

    @Test
    fun `RawTestEntity로 조회하면 Soft Delete된 엔티티도 ID로 조회할 수 있다`() {
        // given
        val id = UUID.randomUUID().toString()
        val entity = testRepositoryImpl.create(
            id = id,
            name = "raw-entity-test"
        )
        testRepository.save(entity)
        testRepository.delete(id)

        // when - RawTestEntity는 @SQLRestriction이 없으므로 Soft Delete된 엔티티도 조회됨
        val found = rawTestJpaRepository.findById(id).orElse(null)

        // then
        assertNotNull(found, "RawTestEntity를 사용하면 Soft Delete된 엔티티도 조회되어야 합니다")
        assertEquals(id, found?.id)
        assertNotNull(found?.deletedAt, "Soft Delete된 엔티티는 deletedAt이 설정되어 있어야 합니다")
    }

    @Test
    fun `RawTestEntity로 전체 조회하면 Soft Delete된 엔티티도 포함된다`() {
        // given
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()
        val entity1 = testRepositoryImpl.create(
            id = id1,
            name = "raw-entity-all-test-1"
        )
        val entity2 = testRepositoryImpl.create(
            id = id2,
            name = "raw-entity-all-test-2"
        )
        testRepository.save(entity1)
        testRepository.save(entity2)
        testRepository.delete(id1)

        // when - RawTestEntity는 @SQLRestriction이 없으므로 Soft Delete된 엔티티도 조회됨
        val all = rawTestJpaRepository.findAll()

        // then
        assertTrue(all.any { it.id == id1 }, "RawTestEntity를 사용하면 Soft Delete된 엔티티도 조회되어야 합니다")
        assertTrue(all.any { it.id == id2 }, "삭제되지 않은 엔티티는 조회되어야 합니다")
        
        val deletedEntity = all.find { it.id == id1 }
        assertNotNull(deletedEntity?.deletedAt, "Soft Delete된 엔티티는 deletedAt이 설정되어 있어야 합니다")
    }

    @Test
    fun `RawTestEntity로 이름으로 조회하면 Soft Delete된 엔티티도 조회할 수 있다`() {
        // given
        val id = UUID.randomUUID().toString()
        val name = "raw-entity-name-test"
        val entity = testRepositoryImpl.create(
            id = id,
            name = name
        )
        testRepository.save(entity)
        testRepository.delete(id)

        // when - RawTestEntity는 @SQLRestriction이 없으므로 Soft Delete된 엔티티도 조회됨
        val found = rawTestJpaRepository.findByName(name)

        // then
        assertNotNull(found, "RawTestEntity를 사용하면 Soft Delete된 엔티티도 조회되어야 합니다")
        assertEquals(id, found?.id)
        assertNotNull(found?.deletedAt, "Soft Delete된 엔티티는 deletedAt이 설정되어 있어야 합니다")
    }
}
