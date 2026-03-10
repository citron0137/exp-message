package site.rahoon.message.monolithic.common.controller.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Spring Security 설정
 * Cookie auth는 엄격한 CORS 정책을 전제로 합니다.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig(
    @param:Value("\${security.cors.allowed-origin-patterns}")
    private val allowedOriginPatternsProperty: String,
    @param:Value("\${security.cors.allow-credentials:true}")
    private val allowCredentials: Boolean,
) {
    companion object {
        private const val CORS_MAX_AGE_SECONDS = 3600L
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { }
            .authorizeHttpRequests { auth ->
                auth.anyRequest().permitAll()
            }
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val allowedOriginPatterns =
            allowedOriginPatternsProperty
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

        val configuration = CorsConfiguration().apply {
            this.allowedOriginPatterns = allowedOriginPatterns
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("Authorization")
            this.allowCredentials = this@SecurityConfig.allowCredentials
            maxAge = CORS_MAX_AGE_SECONDS
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
