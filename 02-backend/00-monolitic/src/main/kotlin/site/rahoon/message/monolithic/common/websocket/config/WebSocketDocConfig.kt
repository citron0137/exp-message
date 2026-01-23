package site.rahoon.message.monolithic.common.websocket.config

//import io.github.springwolf.asyncapi.v3.model.AsyncAPI
//import io.github.springwolf.asyncapi.v3.model.components.Components
//import io.github.springwolf.asyncapi.v3.model.info.Info
//import io.github.springwolf.asyncapi.v3.model.security_scheme.HttpSecurityScheme
//import io.github.springwolf.asyncapi.v3.model.security_scheme.SecurityScheme
//import io.github.springwolf.asyncapi.v3.model.security_scheme.SecurityType
//import io.github.springwolf.asyncapi.v3.model.server.Server
//import io.github.springwolf.core.asyncapi.AsyncApiCustomizer
//import org.springframework.context.annotation.Configuration
//
//@Configuration
//class WebSocketDocConfig(
////    private vals
//):  AsyncApiCustomizer {
//
//    override fun customize(asyncApi: AsyncAPI?) {
////        asyncAPI!!
////        val description = """
////                **🔐 실시간 메시징 인증 안내**<br>
////                웹소켓 연결 및 구독을 위해 아래 인증 정보가 필수입니다.
////
////                | 위치 | 키 | 값 | 비고 |
////                |:---:|:---:|:---:|:--- |
////                | **Query** | `token` | `String (JWT)` | Handshake 시점에 사용 |
////                | **Header** | `Authorization` | `Bearer {token}` | STOMP CONNECT 프레임용 |
////
////                ---
////            """.trimIndent()
////        asyncAPI.info.description = description
////                val securitySchemes = mapOf(
////                    "bearerAuth" to SecurityScheme.builder()
////                        .type(SecurityType.HTTP)
////                        .description("이게 뭘까..")
////                        .build()
////        //                .scheme("bearer")
////        //                .bearerFormat("JWT")
////        //                .build()
////                )
////        asyncAPI.components.securitySchemes = securitySchemes
//        asyncApi!!
//        // 1. 서버 정보 명시 (protocol: stomp) && 3. 서버에 인증 정보 연결 (서버 정보가 있을 경우)
//        asyncApi.servers = mapOf(
//            "production" to Server.builder().host("127.0.0.1:8080/ws").protocol("stomp").build()
//        )
//        // 2. Security Schemes 정의
//        val components = asyncApi.components ?: Components()
//        val securitySchemes = mapOf(
//            "bearerAuth" to HttpSecurityScheme.httpBuilder()
//                .description("JWT 토큰을 'Bearer {token}' 형식으로 입력하세요.")
//                .bearerFormat("JWT")
//                .scheme("bearer")
//                .build()
//        )
//
//        components.securitySchemes = securitySchemes
//        asyncApi.components = components
//
//        // 3. Security Schema 서버마다 주입
//        val securitySchemaRef = SecurityScheme.builder().ref("#/components/securitySchemes/bearerAuth").build()
//        asyncApi.servers.forEach { it.value.security = listOf(securitySchemaRef)  }
//    }
//
//}
