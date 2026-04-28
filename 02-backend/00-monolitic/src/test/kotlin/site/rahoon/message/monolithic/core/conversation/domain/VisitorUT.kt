package site.rahoon.message.monolithic.core.conversation.domain

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeBlank
import org.junit.jupiter.api.Test

class VisitorUT {
    @Test
    fun `create normalizes optional visitor fields`() {
        // Arrange: Prepare visitor input with blanks and metadata. / 준비: 빈 값과 metadata가 포함된 visitor 입력을 준비한다.
        val metadata = mapOf("plan" to "pro", "" to "ignored")

        // Act: Create a visitor. / 실행: visitor를 생성한다.
        val visitor =
            Visitor.create(
                channelId = "channel-1",
                externalId = " external-1 ",
                displayName = " Alice ",
                email = " alice@example.com ",
                metadata = metadata,
            )

        // Assert: Verify normalized fields and generated identity. / 검증: 정규화된 필드와 생성된 identity를 검증한다.
        visitor.id.shouldNotBeBlank()
        visitor.channelId shouldBe "channel-1"
        visitor.externalId shouldBe "external-1"
        visitor.displayName shouldBe "Alice"
        visitor.email shouldBe "alice@example.com"
        visitor.metadata shouldBe mapOf("plan" to "pro")
    }
}
