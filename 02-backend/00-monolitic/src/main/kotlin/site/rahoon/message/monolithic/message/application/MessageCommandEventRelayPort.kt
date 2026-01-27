package site.rahoon.message.monolithic.message.application

/**
 * 메시지 명령 이벤트 릴레이 포트 (Port)
 *
 * 사용자별로 메시지 이벤트를 전달하는 인터페이스
 * 실제 구현은 infrastructure 레이어에서 제공 (Redis Pub/Sub 등)
 */
interface MessageCommandEventRelayPort {
    /**
     * 특정 사용자들에게 메시지 이벤트 전송
     */
    fun sendToUsers(userIds: List<String>, event: MessageCommandEvent.Send)

    /**
     * 특정 사용자의 메시지 이벤트 구독 시작
     */
    fun subscribe(userId: String)

    /**
     * 특정 사용자의 메시지 이벤트 구독 해제
     */
    fun unsubscribe(userId: String)
}
