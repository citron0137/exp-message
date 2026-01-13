package site.rahoon.message.__monolitic

import com.redis.testcontainers.RedisContainer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Paths
import java.time.Duration

/**
 * JPA Entity와 Flyway 마이그레이션 스키마의 정합성을 검증하는 테스트
 *
 * 1. MySQL, Redis를 Testcontainers로 띄움
 * 2. 01-db-migrations Docker 이미지를 빌드 & 실행하여 마이그레이션 수행
 * 3. ddl-auto=validate로 JPA Entity와 스키마 정합성 검증
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("schema-validation")
class SchemaValidationTest {

    companion object {
        private val network: Network = Network.newNetwork()

        @Container
        @JvmStatic
        val mysql: MySQLContainer<*> = MySQLContainer("mysql:8.0")
            .withNetwork(network)
            .withNetworkAliases("mysql")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_password")
            .withUrlParam("useSSL", "false")
            .withUrlParam("allowPublicKeyRetrieval", "true")

        @Container
        @JvmStatic
        val redis: RedisContainer = RedisContainer("redis:7-alpine")
            .withNetwork(network)
            .withNetworkAliases("redis")

        @JvmStatic
        @BeforeAll
        fun runMigrations() {
            // 01-db-migrations Dockerfile에서 이미지 빌드 & 실행
            val migrationImage = ImageFromDockerfile()
                .withDockerfile(Paths.get("../01-db-migrations/Dockerfile"))

            GenericContainer(migrationImage)
                .withNetwork(network)
                .withEnv("DB_HOST", "mysql")
                .withEnv("DB_PORT", "3306")
                .withEnv("DB_NAME", "test_db")
                .withEnv("DB_USERNAME", "test_user")
                .withEnv("DB_PASSWORD", "test_password")
                .waitingFor(Wait.forLogMessage(".*Started DbMigrationsApplication.*", 1))
                .withStartupTimeout(Duration.ofMinutes(3))
                .start()

            println("=== Migration completed ===")
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // MySQL
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }

            // JPA - validate mode
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.jpa.database-platform") { "org.hibernate.dialect.MySQLDialect" }

            // Redis
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
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
