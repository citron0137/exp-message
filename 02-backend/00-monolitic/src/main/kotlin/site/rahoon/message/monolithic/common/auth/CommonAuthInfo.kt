package site.rahoon.message.monolithic.common.auth

/**
 * 현재 인증된 사용자 정보를 담는 객체
 *
 * @param expiresAtEpochMs JWT exp(만료 시각) 밀리초. WebSocket 세션 만료 검사용. REST에서는 null.
 */
data class CommonAuthInfo(
    val userId: String,
    val sessionId: String? = null,
    val expiresAtEpochMs: Long? = null,
)
