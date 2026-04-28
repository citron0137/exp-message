package site.rahoon.message.monolithic.core.conversation.domain

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class OriginUT {
    @Test
    fun `parse normalizes scheme host and port`() {
        // Arrange: Prepare an origin-like URL with path and mixed case. / 준비: path와 대소문자가 섞인 origin-like URL을 준비한다.
        val rawOrigin = "HTTPS://Acme.COM:8443/path?a=1"

        // Act: Parse the raw origin. / 실행: raw origin을 파싱한다.
        val origin = Origin.parse(rawOrigin)

        // Assert: Verify only scheme host and port remain. / 검증: scheme, host, port만 남는지 검증한다.
        origin?.value shouldBe "https://acme.com:8443"
    }

    @Test
    fun `parse returns null for blank origin`() {
        // Arrange: Prepare a blank origin. / 준비: 빈 origin을 준비한다.
        val rawOrigin = " "

        // Act: Parse the raw origin. / 실행: raw origin을 파싱한다.
        val origin = Origin.parse(rawOrigin)

        // Assert: Verify blank origin is rejected. / 검증: 빈 origin이 거부되는지 검증한다.
        origin.shouldBeNull()
    }

    @Test
    fun `parse returns null for unsupported scheme`() {
        // Arrange: Prepare an origin with unsupported scheme. / 준비: 지원하지 않는 scheme을 가진 origin을 준비한다.
        val rawOrigin = "ftp://acme.com"

        // Act: Parse the raw origin. / 실행: raw origin을 파싱한다.
        val origin = Origin.parse(rawOrigin)

        // Assert: Verify unsupported schemes are rejected. / 검증: 지원하지 않는 scheme이 거부되는지 검증한다.
        origin.shouldBeNull()
    }
}
