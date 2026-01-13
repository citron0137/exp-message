package site.rahoon.message.__monolitic.loginfailure.infrastructure

import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import site.rahoon.message.__monolitic.loginfailure.domain.LoginFailure
import site.rahoon.message.__monolitic.loginfailure.domain.LoginFailureRepository
import java.time.Duration

/**
 * LoginFailureRepository의 Redis 구현체
 */
@Repository
class LoginFailureRepositoryImpl(
    private val redisTemplate: RedisTemplate<String, String>
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
}
