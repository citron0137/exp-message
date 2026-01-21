package site.rahoon.message.monolithic.common.global.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

/**
 * @EnableAsync for @Async 메서드 (예: 이벤트 리스너 비동기 처리).
 */
@Configuration
@EnableAsync
class AsyncConfig
