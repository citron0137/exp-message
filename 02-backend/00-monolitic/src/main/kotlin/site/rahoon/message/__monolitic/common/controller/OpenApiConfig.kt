package site.rahoon.message.__monolitic.common.controller

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.customizers.OperationCustomizer
import org.springdoc.core.utils.SpringDocUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod
import site.rahoon.message.__monolitic.common.global.utils.AuthInfo
import site.rahoon.message.__monolitic.common.global.utils.AuthInfoAffect

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

        // Security Scheme 추가 (Bearer Token)
        if (openAPI.components == null) {
            openAPI.components = Components()
        }
        openAPI.components
            .addSecuritySchemes(
                "bearerAuth",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("JWT 토큰을 사용한 인증. 'Bearer ' 접두사를 포함하여 토큰을 입력하세요.")
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

    /**
     * @AuthInfoAffect 어노테이션이 있는 메소드에 Security를 자동으로 추가하는 Customizer
     */
    @Bean
    fun authInfoAffectOperationCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation, handlerMethod ->
            val method = (handlerMethod as? HandlerMethod)?.method
            if (method != null) {
                // 메소드 레벨 어노테이션 확인
                val methodAnnotation = method.getAnnotation(AuthInfoAffect::class.java)
                // 클래스 레벨 어노테이션 확인
                val classAnnotation = method.declaringClass.getAnnotation(AuthInfoAffect::class.java)
                
                // 메소드 또는 클래스에 @AuthInfoAffect 어노테이션이 있는 경우 Security 추가
                if (methodAnnotation != null || classAnnotation != null) {
                    operation.addSecurityItem(
                        SecurityRequirement().addList("bearerAuth")
                    )
                }
            }
            operation
        }
    }
}

