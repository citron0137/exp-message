package site.rahoon.message.monolithic.core.conversation.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AllowedOriginsUT {
    @Test
    fun `of normalizes origin values`() {
        // Arrange: Prepare duplicated and blank origin values. / 준비: 중복과 빈 값이 포함된 origin 값을 준비한다.
        val values = listOf(" https://acme.com ", "", "https://acme.com", "https://beta.com")

        // Act: Create the allowed origin value object. / 실행: allowed origin value object를 생성한다.
        val allowedOrigins = AllowedOrigins.of(values)

        // Assert: Verify only normalized distinct values remain. / 검증: 정규화된 고유 값만 남는지 검증한다.
        allowedOrigins.values shouldBe listOf("https://acme.com", "https://beta.com")
    }

    @Test
    fun `allows returns true for exact origin`() {
        // Arrange: Prepare an allow-list with one origin. / 준비: 하나의 origin을 가진 allow-list를 준비한다.
        val allowedOrigins = AllowedOrigins.of(listOf("https://acme.com"))

        // Act: Check an exact origin match. / 실행: 정확히 일치하는 origin을 확인한다.
        val result = allowedOrigins.allows(Origin("https://acme.com"))

        // Assert: Verify the origin is allowed. / 검증: 해당 origin이 허용되는지 검증한다.
        result shouldBe true
    }

    @Test
    fun `allows returns true for wildcard origin`() {
        // Arrange: Prepare an allow-list with wildcard. / 준비: wildcard를 가진 allow-list를 준비한다.
        val allowedOrigins = AllowedOrigins.of(listOf("*"))

        // Act: Check any non-blank origin. / 실행: 비어 있지 않은 임의 origin을 확인한다.
        val result = allowedOrigins.allows(Origin("https://acme.com"))

        // Assert: Verify wildcard allows the origin. / 검증: wildcard가 origin을 허용하는지 검증한다.
        result shouldBe true
    }

    @Test
    fun `allows returns false for empty allow-list`() {
        // Arrange: Prepare an empty allow-list. / 준비: 빈 allow-list를 준비한다.
        val allowedOrigins = AllowedOrigins.of(emptyList())

        // Act: Check an origin against deny-all policy. / 실행: deny-all 정책에 대해 origin을 확인한다.
        val result = allowedOrigins.allows(Origin("https://acme.com"))

        // Assert: Verify empty origins deny all requests. / 검증: 빈 origins가 모든 요청을 거부하는지 검증한다.
        result shouldBe false
    }
}
