package site.rahoon.message.monolithic.common.infrastructure.config

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.hibernate.Session
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import site.rahoon.message.monolithic.common.domain.SoftDeleteContext

/**
 * @Transactional 메서드 진입 시 softDeleteFilter를 활성화하는 Aspect.
 *
 * JpaTransactionManager override 대신 AOP를 사용하여,
 * 어떤 트랜잭션 매니저를 사용하든 일관되게 필터가 활성화되도록 합니다.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE) // 트랜잭션 시작 후(내부에서) 실행
class SoftDeleteFilterAspect {
    @Around(
        "@within(org.springframework.transaction.annotation.Transactional) || " +
            "@annotation(org.springframework.transaction.annotation.Transactional) || " +
            "@within(site.rahoon.message.monolithic.common.global.TransactionalRepository)",
    )
    fun enableFilterAndProceed(joinPoint: ProceedingJoinPoint): Any? {
        // 명시적으로 disable {} 블록 내부인 경우 필터를 활성화하지 않음
        if (SoftDeleteContext.isExplicitlyDisabled()) {
            return joinPoint.proceed()
        }
        val em = SoftDeleteContext.getCurrentEntityManager()
        val session = em?.unwrap(Session::class.java)
        session?.enableFilter("softDeleteFilter")
        return joinPoint.proceed()
    }
}
