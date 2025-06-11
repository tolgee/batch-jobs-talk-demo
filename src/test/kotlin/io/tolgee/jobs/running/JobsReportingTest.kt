package io.tolgee.jobs.running

import io.tolgee.jobs.dtos.JobStatus
import io.tolgee.jobs.entity.Job
import io.tolgee.jobs.executeInNewTransaction
import io.tolgee.jobs.service.JobExecutionService
import io.tolgee.jobs.service.JobPersistenceService
import io.tolgee.jobs.service.jobProcessors.JobProcessor
import io.tolgee.jobs.service.queue.JobQueue
import io.tolgee.jobs.testutil.PostgresExtension
import io.tolgee.jobs.testutil.RedisExtension
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.redisson.api.RedissonClient
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
class JobsReportingTest {

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

  @Autowired
  private lateinit var redissonClient: RedissonClient

  private val statuses = mutableListOf<JobStatus>()

  companion object {
    @Suppress("unused")
    @DynamicPropertySource
    @JvmStatic
    fun setProperties(registry: DynamicPropertyRegistry) {
      PostgresExtension.Companion.registerPgProps(registry)
      RedisExtension.Companion.registerPgProps(registry)
      registry.add("jobs.use-redis") { true }
    }
  }

  @BeforeEach
  fun beforeEach() {
    statuses.clear()
    redissonClient.getTopic("job-status").addListener(JobStatus::class.java) { _, status ->
      statuses.add(status)
    }
    jobExecutionService.run()
    done = false
    fail = false
  }


  @AfterEach
  fun afterEach() {
    executeInNewTransaction(transactionManager) {
      @Suppress("SqlConstantExpression")
      entityManager.createNativeQuery("delete from job where 1 = 1").executeUpdate()
    }
    done = true
    jobExecutionService.stop()
  }

  @Test
  fun `job status is reported when starts`() {
    val job = jobPersistenceService.createJob("reporting", listOf())
    jobQueueService.add(job.id)

    waitFor { statuses.size == 1 }

    assertThat(statuses.single().status).isEqualTo(Job.Status.RUNNING)

    done = true

    waitFor { statuses.size == 2 }
  }

  @Test
  fun `job status is reported when succeeds`() {
    val job = jobPersistenceService.createJob("reporting", listOf())
    jobQueueService.add(job.id)
    done = true

    waitFor {
      statuses.size == 2
    }


    assertThat(statuses.last().status).isEqualTo(Job.Status.SUCCEEDED)
  }

  @Test
  fun `job status is reported when fails`() {
    fail = true
    val job = jobPersistenceService.createJob("reporting", listOf())
    jobQueueService.add(job.id)
    done = true

    waitFor {
      statuses.size == 2
    }


    assertThat(statuses.last().status).isEqualTo(Job.Status.FAILED)
  }
}

var fail = false
var done = false

@Component
class ReportingJobProcessor : JobProcessor {
  override val type: String
    get() = "reporting"

  override fun process(job: Job) {
    while (!done) {
      Thread.sleep(100)
    }
    if (fail) {
      throw Exception("I am failing!")
    }
  }
}
