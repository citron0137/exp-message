package site.rahoon.message.monolithic.common.infrastructure.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import site.rahoon.message.monolithic.common.infrastructure.JpaSoftDeleteRepositoryImpl

/**
 * JPA 설정
 * Soft Delete를 위한 Hibernate Filter를 활성화합니다.
 *
 * 1. JpaTransactionManager: 트랜잭션 시작 시 필터 활성화 (주요 경로)
 * 2. SoftDeleteFilterAspect: @Transactional 메서드 진입 시 필터 활성화 (백업)
 *
 * FilterDefinition과 Filter 적용은 SoftDeleteFilterMetadataContributor에서 처리합니다.
 */
@Configuration
@EntityScan(basePackages = ["site.rahoon.message.monolithic"])
@EnableJpaRepositories(
    basePackages = ["site.rahoon.message.monolithic"],
    repositoryBaseClass = JpaSoftDeleteRepositoryImpl::class,
)
@Suppress("EmptyClassBlock")
class JpaConfig
