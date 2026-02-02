package site.rahoon.message.monolithic.common.websocket

/**
 * WebSocket 세션이 끊어질 때 메서드를 호출한다.
 *
 * 인자로 [CommonAuthInfo]를 넘긴다.
 *
 * 메서드 시그니처: `(authInfo: CommonAuthInfo)`
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebsocketDisconnected
