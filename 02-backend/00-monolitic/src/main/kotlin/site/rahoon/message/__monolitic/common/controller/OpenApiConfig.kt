package site.rahoon.message.__monolitic.common.global.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * OpenAPI (Swagger) 설정
 */
@Configuration
class OpenApiConfig {

    @Value("\${SWAGGER_UI_HOST:}")
    private var swaggerHost: String = ""

    @Bean
    fun openAPI(): OpenAPI {
        val openAPI = OpenAPI()
            .info(
                Info()
                    .title("Message API")
                    .description("Message Service API Documentation")
                    .version("1.0.0")
            )

        // Host가 지정된 경우 Server 정보 추가 (쉼표로 구분된 여러 호스트 지원)
        if (swaggerHost.isNotBlank()) {
            val hosts = swaggerHost.split(",").map { it.trim() }.filter { it.isNotBlank() }
            hosts.forEachIndexed { index, host ->
                openAPI.addServersItem(
                    Server()
                        .url(host)
                        .description(if (hosts.size > 1) "API Server ${index + 1}" else "API Server")
                )
            }
        }

        return openAPI
    }
}

