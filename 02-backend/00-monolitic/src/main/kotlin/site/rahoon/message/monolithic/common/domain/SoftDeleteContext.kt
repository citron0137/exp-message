package site.rahoon.message.monolithic.common.domain

import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import org.hibernate.Session
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.stereotype.Component

/**
 * JPA SoftDelete 필터 관리를 위한 헬퍼 클래스
 * SoftDelete 필터를 비활성화한 상태에서 코드 실행을 간소화하는 유틸리티 메소드 제공
 *
 * TODO 추후 initialize와 propagation을 통해 중첩 제어 가능하도록 수정 필요
 */
@Component
class SoftDeleteContext {

    companion object {
        @Volatile
        lateinit var inst: SoftDeleteContext
            private set

        /** disable {} 블록 안에서 호출된 코드에서 isDisabled()가 true가 되도록 하는 스레드 로컬 플래그 (명시적 disable 여부) */
        private val filterDisabledByThread = ThreadLocal.withInitial { false }

        /**
         * SoftDelete 필터를 비활성화한 상태에서 action을 실행합니다.
         * action 실행 후 필터는 자동으로 다시 활성화됩니다.
         *
         * @param action 필터가 비활성화된 상태에서 실행할 작업
         * @return 작업 실행 결과
         */
        fun <T> disable(action: () -> T): T = inst.disable(action)

        /**
         * SoftDelete 필터를 활성화한 상태에서 action을 실행합니다.
         * action 실행 후 필터는 자동으로 원래 상태로 복원됩니다.
         *
         * @param action 필터가 활성화된 상태에서 실행할 작업
         * @return 작업 실행 결과
         */
        fun <T> enable(action: () -> T): T = inst.enable(action)

        /**
         * SoftDelete 필터가 현재 활성화되어 있는지 확인합니다.
         *
         * @return 필터가 활성화되어 있으면 true, 그렇지 않으면 false
         */
        fun isEnabled(): Boolean = inst.isEnabled()

        /**
         * SoftDelete 필터가 현재 비활성화되어 있는지 확인합니다.
         *
         * @return 필터가 비활성화되어 있으면 true, 그렇지 않으면 false
         */
        fun isDisabled(): Boolean = inst.isDisabled()

        /**
         * SoftDelete 필터가 명시적으로 비활성화되어 있는지 확인합니다.
         * disable {} 블록 내부에서 실행 중일 때만 true를 반환합니다.
         *
         * @return disable {} 블록 내부이면 true, 그렇지 않으면 false
         */
        fun isExplicitlyDisabled(): Boolean = filterDisabledByThread.get()

        /**
         * 현재 트랜잭션에 바인딩된 EntityManager를 반환합니다.
         * EMF 불일치(멀티 컨텍스트/테스트 등) 문제 회피를 위해
         * TransactionSynchronizationManager에서 직접 조회합니다.
         */
        fun getCurrentEntityManager(): EntityManager? = inst.getCurrentEntityManager()
    }

    @PostConstruct
    fun init() {
        inst = this
    }

    /**
     * SoftDelete 필터를 비활성화한 상태에서 action을 실행합니다.
     * action 실행 후 필터는 자동으로 다시 활성화됩니다.
     *
     * @param action 필터가 비활성화된 상태에서 실행할 작업
     * @return 작업 실행 결과
     */
    fun <T> disable(action: () -> T): T {
        val filterName = "softDeleteFilter"
        val entityManager = getCurrentEntityManager()
            ?: throw IllegalStateException("현재 활성화된 트랜잭션이 없습니다. 트랜잭션 내에서 호출해야 합니다.")

        val session = entityManager.unwrap(Session::class.java)
        val wasEnabled = session.getEnabledFilter(filterName) != null

        return try {
            filterDisabledByThread.set(true)
            if (wasEnabled) session.disableFilter(filterName)
            action()
        } finally {
            filterDisabledByThread.set(false)
            if (wasEnabled) session.enableFilter(filterName)
        }
    }

    /**
     * SoftDelete 필터를 활성화한 상태에서 action을 실행합니다.
     * action 실행 후 필터는 자동으로 원래 상태로 복원됩니다.
     *
     * @param action 필터가 활성화된 상태에서 실행할 작업
     * @return 작업 실행 결과
     */
    fun <T> enable(action: () -> T): T {
        val filterName = "softDeleteFilter"
        val entityManager = getCurrentEntityManager()
            ?: throw IllegalStateException("현재 활성화된 트랜잭션이 없습니다. 트랜잭션 내에서 호출해야 합니다.")

        val session = entityManager.unwrap(Session::class.java)
        val wasEnabled = session.getEnabledFilter(filterName) != null
        return try {
            if (!wasEnabled) session.enableFilter(filterName)
            action()
        } finally {
            if (!wasEnabled) session.disableFilter(filterName)
        }
    }

    /**
     * SoftDelete 필터가 현재 활성화되어 있는지 확인합니다.
     *
     * @return 필터가 활성화되어 있으면 true, 그렇지 않으면 false
     */
    fun isEnabled(): Boolean {
        val filterName = "softDeleteFilter"
        val entityManager = getCurrentEntityManager() ?: return false
        val session = entityManager.unwrap(Session::class.java)
        return session.getEnabledFilter(filterName) != null
    }

    /**
     * SoftDelete 필터가 현재 비활성화되어 있는지 확인합니다.
     * disable {} 블록 안에서는 ThreadLocal 플래그로 항상 true를 반환합니다.
     *
     * @return 필터가 비활성화되어 있으면 true, 그렇지 않으면 false
     */
    fun isDisabled(): Boolean = filterDisabledByThread.get() || !isEnabled()

    /**
     * TransactionSynchronizationManager에 바인딩된 EntityManagerHolder에서 EM 조회.
     * EMF 키 불일치 없이 현재 트랜잭션의 EM을 가져옴.
     */
    private fun getCurrentEntityManager(): EntityManager? {
        val resourceMap = TransactionSynchronizationManager.getResourceMap()
        for (value in resourceMap.values) {
            if (value is EntityManagerHolder) {
                return value.entityManager
            }
        }
        return null
    }
}
