package site.rahoon.message.monolithic.common.websocket.config.subscribe

import org.slf4j.LoggerFactory
import org.springframework.aop.support.AopUtils
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.auth.CommonAuthInfo
import site.rahoon.message.monolithic.common.websocket.annotation.WebsocketDisconnected
import site.rahoon.message.monolithic.common.websocket.annotation.WebsocketSubscribe
import java.lang.reflect.Method
import kotlin.reflect.jvm.isAccessible

/**
 * [WebsocketSubscribe], [WebsocketDisconnected]가 붙은 메서드를 스캔해
 * 구독 허용·세션 끊김 시 해당 메서드를 호출한다.
 */
@Component
class WebSocketAnnotatedMethodInvoker(
    private val applicationContext: ApplicationContext,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private data class SubscribeHandler(
        val pattern: String,
        val regex: Regex,
        val varNames: List<String>,
        val bean: Any,
        val method: Method,
    )

    private val subscribeHandlers: List<SubscribeHandler> by lazy { discoverSubscribeHandlers() }
    private val disconnectHandlers: List<Pair<Any, Method>> by lazy { discoverDisconnectHandlers() }

    fun invokeSubscribe(
        destination: String,
        authInfo: CommonAuthInfo,
    ) {
        for (handler in subscribeHandlers) {
            val match = handler.regex.matchEntire(destination) ?: continue
            val pathVars = handler.varNames.mapIndexed { i, name -> name to match.groupValues[i + 1] }.toMap()
            try {
                handler.method.isAccessible = true
                handler.method.invoke(handler.bean, authInfo, pathVars)
            } catch (e: Exception) {
                log.warn("WebsocketSubscribe 메서드 호출 실패: pattern={}, destination={}, cause={}", handler.pattern, destination, e.message)
            }
        }
    }

    fun invokeDisconnect(authInfo: CommonAuthInfo) {
        for ((bean, method) in disconnectHandlers) {
            try {
                method.isAccessible = true
                method.invoke(bean, authInfo)
            } catch (e: Exception) {
                log.warn("WebsocketDisconnected 메서드 호출 실패: bean={}, cause={}", bean::class.simpleName, e.message)
            }
        }
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private fun discoverSubscribeHandlers(): List<SubscribeHandler> {
        val list = mutableListOf<SubscribeHandler>()
        for (name in applicationContext.beanDefinitionNames) {
            val bean = runCatching { applicationContext.getBean(name) }.getOrNull() ?: continue
            val targetClass = AopUtils.getTargetClass(bean)
            for (method in targetClass.declaredMethods) {
                val ann = method.getAnnotation(WebsocketSubscribe::class.java) ?: continue
                if (method.parameterCount != 2) {
                    log.warn("WebsocketSubscribe 메서드 시그니처 오류: {} (CommonAuthInfo, Map<String, String> 필요)", method)
                    continue
                }
                val (regex, varNames) = patternToRegex(ann.value)
                list.add(SubscribeHandler(ann.value, regex, varNames, bean, method))
            }
        }
        return list
    }

    @Suppress("LoopWithTooManyJumpStatements")
    private fun discoverDisconnectHandlers(): List<Pair<Any, Method>> {
        val list = mutableListOf<Pair<Any, Method>>()
        for (name in applicationContext.beanDefinitionNames) {
            val bean = runCatching { applicationContext.getBean(name) }.getOrNull() ?: continue
            val targetClass = AopUtils.getTargetClass(bean)
            for (method in targetClass.declaredMethods) {
                if (method.getAnnotation(WebsocketDisconnected::class.java) == null) continue
                if (method.parameterCount != 1) {
                    log.warn("WebsocketDisconnected 메서드 시그니처 오류: {} (CommonAuthInfo 필요)", method)
                    continue
                }
                list.add(bean to method)
            }
        }
        return list
    }

    private fun patternToRegex(pattern: String): Pair<Regex, List<String>> {
        val varNameRegex = Regex("\\{([^}]+)}")
        val varNames = varNameRegex.findAll(pattern).map { it.groupValues[1] }.toList()
        val parts = pattern.split(varNameRegex)
        val escapedParts = parts.dropLast(1).zip(varNames).joinToString("") { (part, _) ->
            Regex.escape(part) + "([^/]+)"
        }
        val regexStr = "^" + escapedParts + Regex.escape(parts.last()) + "$"
        return Regex(regexStr) to varNames
    }
}
