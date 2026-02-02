package site.rahoon.message.monolithic.common.websocket

/**
 * 해당 destination이 구독될 때 메서드를 호출한다.
 *
 * value에 destination format을 지정 (예: `/topic/user/{userId}/messages`).
 * 구독 허용 시 인자로 [CommonAuthInfo]와 format 변수 Map(변수명 → 값)을 넘긴다.
 *
 * 메서드 시그니처: `(authInfo: CommonAuthInfo, pathVariables: Map<String, String>)`
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebsocketSubscribe(
    val value: String,
)
