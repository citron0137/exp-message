package site.rahoon.message.__monolitic.test.domain

import site.rahoon.message.__monolitic.test.infrastructure.TestEntity

/**
 * TestEntity를 위한 Repository 인터페이스
 * Soft Delete 기능 테스트를 위한 인터페이스입니다.
 */
interface TestRepository {
    fun save(entity: TestEntity): TestEntity
    fun findById(id: String): TestEntity?
    fun findByName(name: String): TestEntity?
    fun findAll(): List<TestEntity>
    fun delete(id: String)
    fun findWithDescriptionLike(descriptionPattern: String): List<TestEntity>
}
