package site.rahoon.message.__monolitic.loginfailure.domain

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * LoginFailureTracker Race Condition 테스트
 * 
 * 문제 상황:
 * - 여러 스레드가 동시에 incrementFailureCount()를 호출할 때,
 *   read-modify-write 패턴을 사용하므로 race condition이 발생할 수 있음
 * - 예: Thread 1과 Thread 2가 동시에 count=4를 읽고, 각각 5로 증가시켜 저장하면
 *   실제로는 6번째 실패가 발생해도 잠금이 되지 않을 수 있음
 */
@SpringBootTest
@Testcontainers
class LoginFailureTrackerRaceConditionTest {

    companion object {
        @Container
        @JvmStatic
        val redisContainer = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("spring.data.redis.password") { "" }
        }
    }

    @Autowired
    private lateinit var loginFailureTracker: LoginFailureTracker

    @Autowired
    private lateinit var loginFailureRepository: LoginFailureRepository

    @BeforeEach
    fun setUp() {
        loginFailureRepository.deleteByKey("race-direct@example.com")
        loginFailureRepository.deleteByKey("127.0.0.1")
    }

    private fun executeConcurrentIncrements(
        email: String,
        ipAddress: String,
        threadCount: Int
    ) {
        val executor = Executors.newFixedThreadPool(threadCount)
        val latch = CountDownLatch(threadCount)

        repeat(threadCount) {
            executor.submit {
                try { latch.countDown(); latch.await(); loginFailureTracker.incrementFailureCount(email, ipAddress) }
                catch (e: Exception) { e.printStackTrace() }
            }
        }

        executor.shutdown()
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS), "모든 스레드가 30초 내에 완료되어야 합니다")
    }

    /**
     * 더 극단적인 Race Condition 테스트
     * 
     * 시나리오:
     * 1. 0번 실패한 상태에서 시작
     * 2. 20개의 스레드가 동시에 incrementFailureCount()를 직접 호출
     * 3. 예상: 최종 실패 횟수는 20
     * 4. 실제: race condition으로 인해 20보다 작을 수 있음
     */
    @Test
    fun `race condition 테스트 - incrementFailureCount 직접 호출 시 실패 횟수가 누락될 수 있음`() {
        // Given: 초기 상태 설정 (0번 실패)
        val email = "race-direct@example.com"
        val ipAddress = "127.0.0.1"
        val threadCount = 20

        loginFailureRepository.deleteByKey(email)
        loginFailureRepository.deleteByKey(ipAddress)
        val initialCount = loginFailureRepository.findByKey(email).failureCount
        assertEquals(0, initialCount, "실패 횟수는 0 여야 합니다. 실제: $initialCount")

        // When: 20개의 스레드가 동시에 incrementFailureCount() 호출
        executeConcurrentIncrements(email, ipAddress, threadCount)

        // Then: 최종 실패 횟수 확인 및 검증
        val finalFailureCount = loginFailureRepository.findByKey(email).failureCount

        println("예상 실패 횟수: $threadCount")
        println("실제 실패 횟수: $finalFailureCount")

        if (finalFailureCount < threadCount) {
            val lostCount = threadCount - finalFailureCount
            println("⚠️ RACE CONDITION 발생! ${lostCount}개의 실패 횟수 증가가 누락되었습니다")
            println("=".repeat(60))
            println("경고: Race Condition이 감지되었습니다!")
            println("여러 스레드가 동시에 같은 값을 읽고 증가시켜")
            println("일부 증가가 덮어써졌습니다.")
            println("=".repeat(60))
        }

        assertTrue(finalFailureCount >= 1, "실패 횟수는 최소 1 이상이어야 합니다. 실제: $finalFailureCount")
    }
}
