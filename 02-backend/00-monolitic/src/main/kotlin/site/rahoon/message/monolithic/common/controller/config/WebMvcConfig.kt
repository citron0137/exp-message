package site.rahoon.message.monolithic.common.controller.config

import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import site.rahoon.message.monolithic.common.controller.filter.CommonAuthInfoArgumentResolver
import site.rahoon.message.monolithic.presentation.http.auth.CoreAuthArgumentResolver

/**
 * WebMvc 설정
 * ArgumentResolver를 등록하여 AuthInfo 파라미터를 자동으로 주입합니다.
 */
@Configuration
class WebMvcConfig(
    private val commonAuthInfoArgumentResolver: CommonAuthInfoArgumentResolver,
    private val coreAuthArgumentResolver: CoreAuthArgumentResolver,
) : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        // Register core auth first so new presentation controllers do not use legacy auth.
        resolvers.add(0, coreAuthArgumentResolver)
        // Register legacy auth after core auth for existing controllers.
        resolvers.add(1, commonAuthInfoArgumentResolver)
    }
}
