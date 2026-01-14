package site.rahoon.message.__monolitic.common.test

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * JPA Entity와 Flyway 마이그레이션 스키마의 정합성을 검증하는 테스트
 *
 * 1. MySQL, Redis를 Testcontainers로 띄움 (IntegrationTestBase)
 * 2. 01-db-migrations Docker 이미지를 빌드 & 실행하여 마이그레이션 수행 (자동)
 * 3. ddl-auto=validate로 JPA Entity와 스키마 정합성 검증
 */
@IntegrationTest
@ActiveProfiles("schema-validation")
class SchemaValidationTest : IntegrationTestBase() {

    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun schemaValidationProperties(registry: DynamicPropertyRegistry) {
            // JPA - validate mode (스키마 검증용)
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
        }
    }

    @Test
    fun `Flyway 마이그레이션 스키마와 JPA Entity가 일치한다`() {
        // Spring Context가 정상적으로 로드되면 테스트 통과
        // ddl-auto=validate 이므로 스키마 불일치 시 Context 로드 실패
        assertDoesNotThrow {
            println("=== Schema Validation Passed ===")
            println("MySQL: ${mysql.jdbcUrl}")
            println("Redis: ${redis.host}:${redis.firstMappedPort}")
        }
    }
}
