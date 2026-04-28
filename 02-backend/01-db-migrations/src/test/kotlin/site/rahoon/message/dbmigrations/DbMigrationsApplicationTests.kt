package site.rahoon.message.dbmigrations

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class DbMigrationsApplicationTests {
    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val mysql: MySQLContainer<*> =
            MySQLContainer("mysql:8.0")
                .withDatabaseName("test_db")
                .withUsername("test_user")
                .withPassword("test_password")
                .withUrlParam("useSSL", "false")
                .withUrlParam("allowPublicKeyRetrieval", "true")
    }

    @Test
    fun contextLoads() {
    }
}
