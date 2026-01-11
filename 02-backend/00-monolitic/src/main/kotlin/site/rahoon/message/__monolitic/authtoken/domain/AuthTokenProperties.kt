package site.rahoon.message.__monolitic.authtoken.domain

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "authtoken")
data class AuthTokenProperties(
    /**
     * 리프레시 토큰 만료(초)
     */
    val refreshTokenTtlSeconds: Long = 60L * 60L * 24L * 14L // 14 days
)

