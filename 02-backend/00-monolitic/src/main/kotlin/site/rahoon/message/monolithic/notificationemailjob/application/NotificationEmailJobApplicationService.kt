package site.rahoon.message.monolithic.notificationemailjob.application

import org.springframework.stereotype.Service
import site.rahoon.message.monolithic.notificationemailjob.application.component.NotificationEmailSender
import site.rahoon.message.monolithic.notificationemailjob.domain.NotificationEmailJob

@Service
class NotificationEmailJobApplicationService(
    private val notificationEmailSender: NotificationEmailSender,
) {
    fun create(criteria: NotificationEmailJobCriteria.Create): NotificationEmailJob {
        val job = NotificationEmailJob(
            criteria.notificationId,
            criteria.email,
            criteria.subject,
            criteria.content,
            0,
            criteria.maxAttempts,
            null,
            null,
        )
        notificationEmailSender.send(job)
        return job
    }

    fun createAll(criteriaList: List<NotificationEmailJobCriteria.Create>) {
        val jobs = criteriaList.map { criteria ->
            NotificationEmailJob(
                criteria.notificationId,
                criteria.email,
                criteria.subject,
                criteria.content,
                0,
                criteria.maxAttempts,
                null,
                null,
            )
        }
        jobs.forEach { job ->
            runCatching {
                notificationEmailSender.send(job)
            }
        }
    }
}
