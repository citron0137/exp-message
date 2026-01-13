package site.rahoon.message.__monolitic.loginfailure.infrastructure

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import site.rahoon.message.__monolitic.loginfailure.domain.LoginFailure
import site.rahoon.message.__monolitic.loginfailure.domain.LoginFailureRepository
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * LoginFailureRepository의 Redis 구현체
 */
@Repository
class LoginFailureRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, String>,
    private val redisTemplateLong: RedisTemplate<String, Long>
) : LoginFailureRepository {

    companion object {
        private const val FAILURE_COUNT_PREFIX = "login_failure:"
    }

    override fun findByKey(key: String): LoginFailure {
        val redisKey = "$FAILURE_COUNT_PREFIX$key"
        val count = redisTemplate.opsForValue().get(redisKey) ?: return LoginFailure.create(key)
        val failureCount = count.toIntOrNull() ?: 0
        return LoginFailure.from(key, failureCount)
    }

    override fun save(loginFailure: LoginFailure, ttl: Duration): LoginFailure {
        val redisKey = "$FAILURE_COUNT_PREFIX${loginFailure.key}"
        redisTemplate.opsForValue().set(redisKey, loginFailure.failureCount.toString(), ttl)
        return loginFailure
    }

    override fun deleteByKey(key: String) {
        val redisKey = "$FAILURE_COUNT_PREFIX$key"
        redisTemplate.delete(redisKey)
    }

    override fun incrementAndGet(key: String, ttl: Duration): Int {
        val redisKey = "$FAILURE_COUNT_PREFIX$key"
        val newValue = redisTemplateLong.opsForValue().increment(redisKey) ?: 1L
        
        // TTL 설정 (키가 새로 생성된 경우에만)
        if (newValue == 1L) {
            redisTemplateLong.expire(redisKey, ttl.toSeconds(), TimeUnit.SECONDS)
        } else {
            // 기존 키의 TTL 갱신 (남은 시간이 ttl보다 작으면 갱신)
            val currentTtl = redisTemplateLong.getExpire(redisKey, TimeUnit.SECONDS)
            if (currentTtl == null || currentTtl < ttl.toSeconds()) {
                redisTemplateLong.expire(redisKey, ttl.toSeconds(), TimeUnit.SECONDS)
            }
        }
        
        return newValue.toInt()
    }
}
