package site.rahoon.message.__monolitic.common.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    /**
     * HMAC 서명에 사용할 시크릿 키 (HS256 기준 최소 32바이트 권장)
     */
    val secret: String,
    val issuer: String = "site.rahoon.message",
    val accessTokenTtlSeconds: Long = 60 * 60
)

