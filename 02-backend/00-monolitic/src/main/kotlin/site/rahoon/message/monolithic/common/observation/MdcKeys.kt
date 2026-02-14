package site.rahoon.message.monolithic.common.observation

/**
 * 구조화 로깅용 MDC 키 상수.
 * HttpRequestObservationHandler, CommonAuthInfoArgumentResolver 등에서 공유.
 */
object MdcKeys {
    const val HTTP_METHOD = "http.method"
    const val HTTP_PATH = "http.path"
    const val HTTP_STATUS = "http.status_code"
    const val HTTP_DURATION_MS = "http.duration_ms"
    const val HTTP_START_TIME = "http.start_time"
    const val HTTP_END_TIME = "http.end_time"
    const val CLIENT_IP = "client_ip"
    const val USER_AGENT = "user_agent"
    const val USER_ID = "user_id"
    const val SESSION_ID = "session_id"
}
