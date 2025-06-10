package io.tolgee.jobs.testutil

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer

class PostgresExtension : BeforeAllCallback, AfterAllCallback {
  override fun beforeAll(context: ExtensionContext?) {
    // Start PostgreSQL container
    postgresContainer = PostgreSQLContainer("postgres:17-alpine")
      .withDatabaseName("testdb")
      .withUsername("test")
      .withPassword("test")

    postgresContainer.start()
  }

  override fun afterAll(context: ExtensionContext?) {
    postgresContainer.stop()
  }

  companion object {
    private lateinit var postgresContainer: PostgreSQLContainer<*>

    /**
     * Expose the live container values to Spring.
     * Anything you add here ends up in Spring’s Environment, exactly
     * as if it had been placed in application-test.yaml.
     */
    @JvmStatic
    fun registerPgProps(registry: DynamicPropertyRegistry) {
      registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
      registry.add("spring.datasource.username") { postgresContainer.username }
      registry.add("spring.datasource.password") { postgresContainer.password }
      // Optional extras
      registry.add("spring.flyway.url") { postgresContainer.jdbcUrl }
      registry.add("spring.flyway.user") { postgresContainer.username }
      registry.add("spring.flyway.password") { postgresContainer.password }
    }
  }
}
