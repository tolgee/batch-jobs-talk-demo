package io.tolgee.jobs.testutil

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName

class RedisExtension : BeforeAllCallback, AfterAllCallback {
  override fun beforeAll(context: ExtensionContext?) {
    redisContainer = GenericContainer(DockerImageName.parse("redis:7-alpine"))
      .withExposedPorts(6379)

    redisContainer.start()
  }

  override fun afterAll(context: ExtensionContext?) {
    redisContainer.stop()
  }

  companion object {
    @Container
    private lateinit var redisContainer: GenericContainer<*>
    /**
     * Expose the live container values to Spring.
     * Anything you add here ends up in Spring’s Environment, exactly
     * as if it had been placed in application-test.yaml.
     */
    @JvmStatic
    fun registerPgProps(registry: DynamicPropertyRegistry) {
      registry.add("spring.data.redis.host") { redisContainer.host }
      registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
    }
  }
}
