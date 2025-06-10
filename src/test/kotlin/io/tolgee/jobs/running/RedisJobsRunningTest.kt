package io.tolgee.jobs.running

import io.tolgee.jobs.testutil.PostgresExtension
import io.tolgee.jobs.testutil.RedisExtension
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@ExtendWith(PostgresExtension::class)
@ExtendWith(RedisExtension::class)
class RedisJobsRunningTest : AbstractJobsRunningTest() {
  companion object {
    @Suppress("unused")
    @DynamicPropertySource
    @JvmStatic
    fun setProperties(registry: DynamicPropertyRegistry) {
      registry.add("jobs.use-redis") { true }
    }
  }
}
