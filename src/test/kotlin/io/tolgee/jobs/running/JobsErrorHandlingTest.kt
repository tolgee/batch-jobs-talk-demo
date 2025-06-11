package io.tolgee.jobs.running

import io.tolgee.jobs.entity.Job
import io.tolgee.jobs.executeInNewTransaction
import io.tolgee.jobs.service.JobExecutionService
import io.tolgee.jobs.service.JobPersistenceService
import io.tolgee.jobs.service.jobProcessors.JobProcessor
import io.tolgee.jobs.service.queue.JobQueue
import io.tolgee.jobs.testutil.PostgresExtension
import io.tolgee.jobs.testutil.RedisExtension
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.stereotype.Component
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.testcontainers.junit.jupiter.Testcontainers
import waitFor

@Testcontainers
@SpringBootTest
@ExtendWith(PostgresExtension::class)
@ExtendWith(RedisExtension::class)
class JobsErrorHandlingTest {
  @Autowired
  private lateinit var transactionManager: PlatformTransactionManager

  @Autowired
  private lateinit var jobQueueService: JobQueue

  @Autowired
  private lateinit var jobPersistenceService: JobPersistenceService

  @Autowired
  private lateinit var jobExecutionService: JobExecutionService

  @Autowired
  private lateinit var entityManager: EntityManager

  companion object {
    @Suppress("unused")
    @DynamicPropertySource
    @JvmStatic
    fun setProperties(registry: DynamicPropertyRegistry) {
      PostgresExtension.Companion.registerPgProps(registry)
      RedisExtension.Companion.registerPgProps(registry)
    }
  }

  @AfterEach
  fun afterEach() {
    executeInNewTransaction(transactionManager) {
      @Suppress("SqlConstantExpression")
      entityManager.createNativeQuery("delete from job where 1 = 1").executeUpdate()
    }
  }

  @Test
  fun `failed jobs ends in failed state`() {
    jobExecutionService.run()
    val job = jobPersistenceService.createJob("failing", listOf())
    jobQueueService.add(job.id)

    waitFor {
      jobPersistenceService.getJob(job.id).status == Job.Status.FAILED
    }

    jobExecutionService.stop()
  }
}

@Component
class FailingJobProcessor : JobProcessor {
  override val type: String
    get() = "failing"

  override fun process(job: Job) {
    throw Exception("I am failing!")
  }
}
