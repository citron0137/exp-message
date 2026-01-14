package site.rahoon.message.__monolitic.common.test

import com.redis.testcontainers.RedisContainer
import org.junit.jupiter.api.BeforeAll
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
 * MySQL + Redis Testcontainers 기반 통합 테스트 베이스 클래스
 *
 * - 컨테이너 시작 후 자동으로 DB 마이그레이션 실행
 * - 모든 통합 테스트에서 동일한 인프라 환경 공유
 *
 * 사용법:
 * ```kotlin
 * @IntegrationTest
 * class MyIntegrationTest : IntegrationTestBase() {
 *     @Test
 *     fun myTest() { ... }
 * }
 * ```
 */
@Testcontainers
abstract class IntegrationTestBase {

    companion object {
        @JvmStatic
        protected val network: Network = Network.newNetwork()

        @JvmStatic
        private var migrationCompleted = false

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
        fun setupMigrations() {
            if (!migrationCompleted) {
                runMigrations()
                migrationCompleted = true
            }
        }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // MySQL
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }
            registry.add("spring.jpa.database-platform") { "org.hibernate.dialect.MySQLDialect" }

            // Redis
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.firstMappedPort }
        }

        /**
         * 01-db-migrations Docker 이미지를 빌드하고 마이그레이션을 실행합니다.
         */
        private fun runMigrations() {
            println("=== Running DB migrations ===")
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
    }
}
