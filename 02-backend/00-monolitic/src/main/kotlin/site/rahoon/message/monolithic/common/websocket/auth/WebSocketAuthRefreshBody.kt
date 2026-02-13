package site.rahoon.message.monolithic.common.websocket.auth

/**
 * `/app/auth/refresh` SEND 요청 Body.
 *
 * Authorization 헤더가 없을 때 [accessToken]으로 토큰 전달.
 *
 * @param accessToken 갱신할 액세스 토큰. null이면 헤더에서만 조회.
 */
data class WebSocketAuthRefreshBody(
    val accessToken: String? = null,
)
