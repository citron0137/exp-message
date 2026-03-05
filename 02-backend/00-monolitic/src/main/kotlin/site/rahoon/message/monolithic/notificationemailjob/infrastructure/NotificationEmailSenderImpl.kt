package site.rahoon.message.monolithic.notificationemailjob.infrastructure

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.global.AsyncRunner
import site.rahoon.message.monolithic.common.global.SpanRunner
import site.rahoon.message.monolithic.notificationemailjob.application.component.NotificationEmailSender
import site.rahoon.message.monolithic.notificationemailjob.domain.NotificationEmailJob
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * NotificationEmailSender 구현체 (실제 이메일 전송)
 *
 * - JavaMailSender를 사용해 메일을 전송한다.
 * - 간단한 재시도, 지수 백오프, 서킷브레이커를 포함한다.
 */
@Component
class NotificationEmailSenderImpl(
    private val mailSender: JavaMailSender,
    @Value("\${notification.email.from:no-reply@example.com}")
    private val fromAddress: String,
) : NotificationEmailSender {
    private val log = LoggerFactory.getLogger(javaClass)

    // 서킷브레이커 상태
    private val consecutiveFailures = AtomicInteger(0)
    private val circuitOpenedAt = AtomicReference<Instant?>(null)

    override fun send(job: NotificationEmailJob) {
        AsyncRunner.runAsync {
            SpanRunner.runWithSpan("notification-email-send") {
                if (!canSendNow()) {
                    log.warn(
                        "Notification 이메일 전송이 서킷브레이커에 의해 차단됨 - notificationId={}, target={}",
                        job.notificationId,
                        job.email,
                    )
                    return@runWithSpan
                }

                sendWithRetry(job)
            }
        }
    }

    private fun canSendNow(): Boolean {
        val openedAt = circuitOpenedAt.get() ?: return true
        val now = Instant.now()
        val elapsed = Duration.between(openedAt, now)
        return if (elapsed >= CIRCUIT_OPEN_DURATION) {
            // 쿨다운 이후에는 다시 닫힌 상태로 전환
            circuitOpenedAt.set(null)
            consecutiveFailures.set(0)
            true
        } else {
            false
        }
    }

    private fun openCircuit() {
        circuitOpenedAt.set(Instant.now())
        log.error(
            "Notification 이메일 전송 서킷브레이커 오픈 - {}초 동안 전송 시도 중단",
            CIRCUIT_OPEN_DURATION.seconds,
        )
    }

    private fun sendWithRetry(initialJob: NotificationEmailJob) {
        var attempt = 0
        var delayMillis = INITIAL_BACKOFF_MILLIS
        var shouldStop = false

        while (attempt < initialJob.maxAttempts && !shouldStop) {
            try {
                attempt++
                log.info(
                    "Notification 이메일 전송 시도 - attempt={}/{} notificationId={} target={}",
                    attempt,
                    initialJob.maxAttempts,
                    initialJob.notificationId,
                    initialJob.email,
                )

                sendOnce(initialJob)

                consecutiveFailures.set(0)
                shouldStop = true
            } catch (e: Exception) {
                log.warn(
                    "Notification 이메일 전송 실패 - attempt={}/{} notificationId={} target={}",
                    attempt,
                    initialJob.maxAttempts,
                    initialJob.notificationId,
                    initialJob.email,
                    e,
                )

                consecutiveFailures.incrementAndGet()
                if (consecutiveFailures.get() >= CIRCUIT_FAILURE_THRESHOLD) {
                    openCircuit()
                    shouldStop = true
                } else if (attempt >= initialJob.maxAttempts) {
                    log.error(
                        "Notification 이메일 전송 최대 재시도 횟수 초과 - notificationId={} target={}",
                        initialJob.notificationId,
                        initialJob.email,
                    )
                    shouldStop = true
                } else {
                    try {
                        Thread.sleep(delayMillis)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        shouldStop = true
                    }
                    delayMillis = (delayMillis * BACKOFF_MULTIPLIER).coerceAtMost(MAX_BACKOFF_MILLIS)
                }
            }
        }
    }

    /**
     * 실제 이메일 전송 1회 수행
     */
    private fun sendOnce(job: NotificationEmailJob) {
        val mimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(mimeMessage, false, StandardCharsets.UTF_8.name())

        helper.setFrom(fromAddress)
        helper.setTo(job.email)
        helper.setSubject(job.subject)
        helper.setText(job.content, false)

        mailSender.send(mimeMessage)
    }

    companion object {
        private const val INITIAL_BACKOFF_MILLIS = 500L
        private const val MAX_BACKOFF_MILLIS = 5_000L
        private const val BACKOFF_MULTIPLIER = 2L

        private const val CIRCUIT_FAILURE_THRESHOLD = 5
        private val CIRCUIT_OPEN_DURATION: Duration = Duration.ofSeconds(30)
    }
}
