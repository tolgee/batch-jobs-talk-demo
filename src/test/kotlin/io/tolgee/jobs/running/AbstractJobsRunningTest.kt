package io.tolgee.jobs.running

import io.tolgee.jobs.entity.Job
import io.tolgee.jobs.executeInNewTransaction
import io.tolgee.jobs.service.JobExecutionService
import io.tolgee.jobs.service.JobPersistenceService
import io.tolgee.jobs.service.JobStartService
import io.tolgee.jobs.service.queue.LocalJobQueue
import io.tolgee.jobs.testutil.PostgresExtension
import io.tolgee.jobs.testutil.RedisExtension
import io.tolgee.jobs.util.UlidGenerator
import jakarta.persistence.EntityManager
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Duration
import java.util.concurrent.TimeUnit

@Testcontainers
@SpringBootTest
@ExtendWith(PostgresExtension::class)
@ExtendWith(RedisExtension::class)
class AbstractJobsRunningTest {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var transactionManager: PlatformTransactionManager

  @Autowired
  private lateinit var jobQueueService: LocalJobQueue

  @Autowired
  private lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  private lateinit var jobPersistenceService: JobPersistenceService

  @Autowired
  private lateinit var jobStartService: JobStartService

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
      entityManager.createNativeQuery("delete from job where 1=1").executeUpdate()
    }
  }

  @Test
  fun `it runs the job`() {
    jobExecutionService.run()
    val job = jobStartService.runJob()

    waitFor {
      jobPersistenceService.getJob(job.id).status == Job.Status.SUCCEEDED
    }

    jobExecutionService.stop()
  }

  @Test
  fun `test throughput`() {
    val count = 10_000L
    batchInsertJobs(count)

    val time = measureTime {
      jobExecutionService.run(10)
      waitForCountSucceeded(count)
    }

    logger.info(
      "Took ${time.toMillis()}ms to run $count jobs which is " +
          "${count * 1000 / time.toMillis()} jobs per second"
    )

    jobExecutionService.stop()
  }

  private fun waitForCountSucceeded(count: Long) {
    waitFor(pollTimeInMs = 100, timeoutInMs = 10000000000L) {
      val processedCount = entityManager.createQuery(
        """select count(j) from Job j where j.status = 'SUCCEEDED'""",
        Long::class.java
      ).singleResult
      logger.info("Processed $processedCount jobs")
      processedCount == count
    }
  }

  private fun batchInsertJobs(count: Long) {
    val args = (1..count).map {
      arrayOf(
        UlidGenerator.generateJobId(),
        "no-op",
        """["John Doe"]""",
        "PENDING"
      )
    }
    jdbcTemplate.batchUpdate(
      "INSERT INTO job (id, job_type, target, status) VALUES (?, ?, ?::jsonb, ?)", args
    )
    val ids = args.map { it[0] }
    ids.forEach {
      jobQueueService.add(it)
    }
  }

  fun waitFor(pollTimeInMs: Int = 100, timeoutInMs: Long = 10000, fn: () -> Boolean) =
    Awaitility.await()
      .pollDelay(pollTimeInMs.toLong(), TimeUnit.MILLISECONDS)
      .timeout(timeoutInMs, TimeUnit.MILLISECONDS)
      .until(fn)


  fun measureTime(fn: () -> Unit): Duration {
    val start = System.currentTimeMillis()
    fn()
    return Duration.ofMillis(System.currentTimeMillis() - start)
  }
}
