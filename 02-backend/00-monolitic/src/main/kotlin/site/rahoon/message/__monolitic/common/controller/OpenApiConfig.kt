package site.rahoon.message.__monolitic.common.controller

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.DelegatingMethodParameterCustomizer
import org.springdoc.core.customizers.ParameterCustomizer
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.MethodParameter
import site.rahoon.message.__monolitic.common.global.utils.AuthInfo

/**
 * OpenAPI (Swagger) 설정
 */
@Configuration
class OpenApiConfig {

    @Value("\${SWAGGER_UI_HOST:}")
    private var swaggerHost: String = ""

    companion object {
        init {
            // AuthInfo 타입의 파라미터를 모든 문서에서 무시하도록 설정
            SpringDocUtils
                .getConfig()
                .addRequestWrapperToIgnore(AuthInfo::class.java)
        }
    }

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

