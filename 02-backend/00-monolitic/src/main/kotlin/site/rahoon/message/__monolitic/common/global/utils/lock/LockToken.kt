package site.rahoon.message.__monolitic.common.global.utils.lock

import java.time.Instant

/**
 * 분산 락 토큰
 * 락 획득 시 반환되며, 해제 시 이 토큰을 사용합니다.
 */
data class LockToken(
    val keys: List<String>,
    val lockId: String,
    val expiresAt: Instant
) {
    constructor(key: String, lockId: String, expiresAt: Instant)
        : this(listOf(key), lockId, expiresAt)

    /**
     * 락이 만료되었는지 확인합니다.
     */
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)
}
