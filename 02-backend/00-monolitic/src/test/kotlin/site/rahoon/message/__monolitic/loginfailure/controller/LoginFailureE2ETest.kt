package site.rahoon.message.__monolitic.loginfailure.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import site.rahoon.message.__monolitic.authtoken.controller.AuthRequest
import site.rahoon.message.__monolitic.common.test.ConcurrentTestUtils
import site.rahoon.message.__monolitic.loginfailure.domain.LoginFailureRepository
import site.rahoon.message.__monolitic.loginfailure.domain.LoginFailureTracker
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * 로그인 실패 추적 Race Condition 테스트
 * 
 * 문제 상황:
 * - 여러 요청이 동시에 들어올 때, incrementFailureCount() 메서드가
 *   read-modify-write 패턴을 사용하므로 race condition이 발생할 수 있음
 * - 예: Thread 1과 Thread 2가 동시에 count=4를 읽고, 각각 5로 증가시켜 저장하면
 *   실제로는 6번째 실패가 발생해도 잠금이 되지 않을 수 있음
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class LoginFailureE2ETest {

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

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var loginFailureTracker: LoginFailureTracker

    @Autowired
    private lateinit var loginFailureRepository: LoginFailureRepository

    @Autowired
    private lateinit var redisTemplate: RedisTemplate<String, String>

    private fun authBaseUrl(): String = "http://localhost:$port/auth"
    private fun userBaseUrl(): String = "http://localhost:$port/users"

    @BeforeEach
    fun setUp() {
        restTemplate.exchange(userBaseUrl(), HttpMethod.POST,
            HttpEntity(objectMapper.writeValueAsString(mapOf("email" to "race-test@example.com", "password" to "password123", "nickname" to "racetest")),
                HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }), String::class.java)
        loginFailureRepository.deleteByKey("race-test@example.com")
        loginFailureRepository.deleteByKey("127.0.0.1")
    }



    /**
     * Race Condition 통합 테스트
     * 
     * 시나리오:
     * 1. 4번 실패한 상태에서 시작
     * 2. 20개 스레드가 동시에 로그인 실패를 시도
     * 3. 검증:
     *    - HTTP 응답: USER_001(로그인 실패) 1개, LOGIN_FAILURE_001(잠금) 19개
     *    - Redis: 실패 카운트 5 이상, TTL은 "테스트 시작 전 시간 + 15분"과 "테스트 마지막 시간 +15분" 사이
     */
    @Test
    fun `race condition 테스트 - 동시 요청 시 실패 횟수와 잠금 동작 검증`() {
        // Given: 4번 실패한 상태로 설정
        val email = "race-test@example.com"
        val ipAddress = "127.0.0.1"
        val initialFailureCount = 4
        val threadCount = 20
        val lockoutDurationMinutes = 15L

        repeat(initialFailureCount) { loginFailureTracker.incrementFailureCount(email, ipAddress) }
        val beforeCount = loginFailureRepository.findByKey(email).failureCount
        assertEquals(initialFailureCount, beforeCount, "실패 횟수는 $initialFailureCount 여야 합니다. 실제: $beforeCount")

        // HttpEntity 미리 생성
        val loginRequestEntity = HttpEntity(
            objectMapper.writeValueAsString(AuthRequest.Login(email = email, password = "wrongpassword")),
            HttpHeaders().apply { contentType = MediaType.APPLICATION_JSON }
        )

        // When: 여러 스레드가 동시에 로그인 실패를 시도
        val testStartTime = Instant.now()
        val responseBodies = ConcurrentTestUtils.executeConcurrent(threadCount) {
            try {
                restTemplate.exchange("${authBaseUrl()}/login", HttpMethod.POST, loginRequestEntity, String::class.java).body
            } catch (e: Exception) {
                null
            }
        }
        val testEndTime = Instant.now()

        // 응답 코드 추출
        val errorCodes = responseBodies.map { responseBody ->
            if (responseBody == null) "UNKNOWN"
            else {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val responseMap = objectMapper.readValue(responseBody, Map::class.java) as Map<String, Any>
                    if (responseMap["success"] == true) null
                    else (responseMap["error"] as? Map<*, *>)?.get("code") as? String ?: "UNKNOWN"
                } catch (e: Exception) {
                    "UNKNOWN"
                }
            }
        }
        
        // 응답 코드 통계 수집
        val user001Count = errorCodes.count { it == "USER_001" } // 로그인 실패
        val loginFailure001Count = errorCodes.count { it == "LOGIN_FAILURE_001" } // 잠금
        val otherErrorCount = errorCodes.count { it != "USER_001" && it != "LOGIN_FAILURE_001" && it != null } // 기타 에러

        // Then: 실제 값 출력
        val finalFailureCount = loginFailureRepository.findByKey(email).failureCount
        val redisKey = "login_failure:$email"
        val ttlSeconds = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS)
        val checkTime = Instant.now()
        val actualExpireTime = if (ttlSeconds > 0) checkTime.plusSeconds(ttlSeconds) else null
        
        // TTL 검증: TTL이 설정된 시점(testStartTime ~ testEndTime)부터 15분 후가 되어야 함
        // 여유 시간 2초 추가 (네트워크 지연, TTL 조회 시점 오차 등 고려)
        val expectedMinExpireTime = testStartTime.plus(Duration.ofMinutes(lockoutDurationMinutes)).minusSeconds(2)
        val expectedMaxExpireTime = testEndTime.plus(Duration.ofMinutes(lockoutDurationMinutes)).plusSeconds(2)

        println("\n" + "=".repeat(60))
        println("Race Condition 테스트 결과")
        println("=".repeat(60))
        println("HTTP 응답 통계:")
        println("  USER_001(로그인 실패): $user001Count")
        println("  LOGIN_FAILURE_001(잠금): $loginFailure001Count")
        println("  기타 에러: $otherErrorCount")
        println("\nRedis 결과:")
        println("  실패 카운트: $finalFailureCount")
        println("  TTL (초): $ttlSeconds")
        if (actualExpireTime != null) {
            println("  예상 만료 시간: $actualExpireTime")
            println("  예상 범위: $expectedMinExpireTime ~ $expectedMaxExpireTime")
        }
        println("=".repeat(60))

        // Then: HTTP 응답 검증
        assertTrue(user001Count <= 1, "USER_001(로그인 실패)는 1개 이하여야 합니다. 실제: $user001Count")
        assertTrue(loginFailure001Count >= 19, "LOGIN_FAILURE_001(잠금)은 19개 이상이어야 합니다. 실제: $loginFailure001Count")
        assertEquals(0, otherErrorCount, "기타 에러는 0개여야 합니다. 실제: $otherErrorCount")

        // Then: Redis 결과 검증
        assertTrue(finalFailureCount >= 5, "실패 카운트는 5 이상이어야 합니다. 실제: $finalFailureCount")
        assertTrue(ttlSeconds > 0, "TTL이 설정되어 있어야 합니다. 실제: $ttlSeconds")
        if (actualExpireTime != null) {
            assertTrue(
                !actualExpireTime.isBefore(expectedMinExpireTime) && !actualExpireTime.isAfter(expectedMaxExpireTime),
                "TTL은 테스트 시작 시간 + 15분 - 2초와 테스트 종료 시간 + 15분 + 2초 사이여야 합니다. " +
                "예상 범위: $expectedMinExpireTime ~ $expectedMaxExpireTime, 실제 만료 시간: $actualExpireTime"
            )
        }
    }
}
