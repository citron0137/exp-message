package site.rahoon.message.__monolitic.loginfailure.domain

import java.time.Duration

/**
 * 로그인 실패 횟수 저장소 인터페이스
 */
interface LoginFailureRepository {
    /**
     * LoginFailure를 조회합니다.
     * @param key Redis 키 (이메일 또는 IP 주소)
     * @return LoginFailure (없으면 실패 횟수 0인 객체)
     */
    fun findByKey(key: String): LoginFailure

    /**
     * LoginFailure를 저장합니다.
     * @param loginFailure 저장할 LoginFailure
     * @param ttl TTL (Time To Live)
     * @return 저장된 LoginFailure
     */
    fun save(loginFailure: LoginFailure, ttl: Duration): LoginFailure

    /**
     * LoginFailure를 삭제합니다.
     * @param key Redis 키 (이메일 또는 IP 주소)
     */
    fun deleteByKey(key: String)
}
