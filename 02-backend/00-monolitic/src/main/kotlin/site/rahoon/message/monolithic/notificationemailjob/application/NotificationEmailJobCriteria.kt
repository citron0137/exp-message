package site.rahoon.message.monolithic.notificationemailjob.application

class NotificationEmailJobCriteria {
    data class Create(
        val notificationId: String,
        val email: String,
        val subject: String,
        val content: String,
        val maxAttempts: Int = 3,
    )
}
