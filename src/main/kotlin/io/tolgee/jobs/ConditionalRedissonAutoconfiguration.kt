package io.tolgee.jobs

import org.redisson.spring.starter.RedissonAutoConfigurationV2
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["jobs.use-redis"], havingValue = "true", matchIfMissing = false)
class ConditionalRedissonAutoconfiguration : RedissonAutoConfigurationV2()
