package site.rahoon.message.monolithic.presentation.http.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import site.rahoon.message.monolithic.core.iam.access.application.facade.AccessFacade
import site.rahoon.message.monolithic.core.iam.access.application.model.AuthenticatedPrincipal
import site.rahoon.message.monolithic.core.iam.exception.AccessError
import site.rahoon.message.monolithic.core.iam.exception.AccessException

@Component
class CoreAuthArgumentResolver(
    private val accessFacade: AccessFacade,
) : HandlerMethodArgumentResolver {
    /**
     * Returns true for parameters that require a core authenticated principal.
     */
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.parameterType == AuthenticatedPrincipal::class.java ||
            parameter.parameterType == AuthenticatedPrincipal::class.javaObjectType

    /**
     * Resolves the authenticated principal from the Authorization header.
     */
    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any {
        val request =
            webRequest.getNativeRequest(HttpServletRequest::class.java)
                ?: throw IllegalStateException("HttpServletRequest is missing")
        val authorization =
            request.getHeader("Authorization")
                ?: throw AccessException(AccessError.AUTHORIZATION_HEADER_MISSING)
        return accessFacade.verifyAccessToken(authorization)
    }
}
