package site.rahoon.message.__monolitic.common.test

import org.junit.jupiter.api.Tag
import org.springframework.boot.test.context.SpringBootTest

/**
 * 단위 테스트 마커 어노테이션
 *
 * - 외부 의존성 없음 (DB, Redis 등)
 * - 빠른 실행 속도
 * - Mocking 기반
 *
 * 실행: `./gradlew unitTest`
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("unit")
annotation class UnitTest

/**
 * 통합 테스트 마커 어노테이션
 *
 * - Testcontainers 사용 (MySQL, Redis 등)
 * - 전체 애플리케이션 컨텍스트 로드 (RANDOM_PORT)
 * - HTTP 요청/응답 + DB 연동 테스트
 *
 * 실행: `./gradlew integrationTest`
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
annotation class IntegrationTest
