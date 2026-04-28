package site.rahoon.message.monolithic.core.iam.access.infrastructure.persistence.loginfailure

import org.springframework.data.jpa.repository.JpaRepository

interface CoreLoginFailureJpaRepository : JpaRepository<CoreLoginFailureEntity, String>
