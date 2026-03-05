package site.rahoon.message.monolithic.notification.application

/**
 * Notification Application 계층에서 사용하는 Criteria
 *
 * - 다양한 도메인에서 재사용 가능하도록, 수신자·템플릿 정보를 추상화한다.
 */
object NotificationCriteria {
    /**
     * Notification EMAIL 생성용 Create Criteria
     *
     * @param recipientUserIds 수신 대상 유저 ID 목록
     * @param subject 기본 제목 템플릿. `{recipientNickname}` 플레이스홀더를 포함할 수 있다.
     * @param content 기본 본문 템플릿. `{recipientNickname}` 플레이스홀더를 포함할 수 있다.
     * @param email EMAIL 채널 전용 템플릿 (있으면 EMAIL 채널에서 우선 사용)
     */
    data class Create(
        val recipientUserIds: List<String>,
        val subject: String,
        val content: String,
        val email: EmailTemplate? = null,
    )

    /**
     * EMAIL 채널 전용 템플릿 정의
     *
     * @param subject 이메일 제목 템플릿. `{recipientNickname}` 플레이스홀더를 포함할 수 있다.
     * @param content 이메일 본문 템플릿. `{recipientNickname}` 플레이스홀더를 포함할 수 있다.
     */
    data class EmailTemplate(
        val subject: String,
        val content: String,
    )
}
