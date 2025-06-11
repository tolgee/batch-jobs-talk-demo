package io.tolgee.jobs.service

import io.tolgee.jobs.entity.Job
import io.tolgee.jobs.executeInNewTransaction
import io.tolgee.jobs.repository.JobRepository
import io.tolgee.jobs.testutil.PostgresExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.CountDownLatch

@Testcontainers
@SpringBootTest
@ExtendWith(PostgresExtension::class)
class JobPersistenceServiceTest {

  companion object {
    @Suppress("unused")
    @DynamicPropertySource
    @JvmStatic
    fun setProperties(registry: DynamicPropertyRegistry) {
      PostgresExtension.registerPgProps(registry)
    }
  }

  @Autowired
  private lateinit var transactionManager: PlatformTransactionManager

  @Autowired
  private lateinit var jobRepository: JobRepository

  @Autowired
  private lateinit var jobPersistenceService: JobPersistenceService

  @Test
  fun testCreateJob() {
    val job = createJob()
    assertThat(jobRepository.findById(job.id)).isNotNull
  }

  @Test
  fun testGetJobWithLocking() {
    runBlocking {
      val jobId = createJob().id

      var done = false
      val countDownLatch = CountDownLatch(2)
      // Launch two coroutines that each execute in a new transaction
      launch(Dispatchers.IO) {
        executeInNewTransaction(transactionManager) {
          assertThat(jobPersistenceService.getJobWithLocking(jobId)).isNotNull
          countDownLatch.countDown()
          while (!done) {
            Thread.sleep(10)
          }
        }
      }

      Thread.sleep(100)

      launch(Dispatchers.IO) {
        executeInNewTransaction(transactionManager) {
          assertThat(jobPersistenceService.getJobWithLocking(jobId)).isNull()
          countDownLatch.countDown()
          done = true
        }
      }

      Thread.sleep(200)
      assertThat(countDownLatch.count).isEqualTo(0)
    }
  }

  private fun createJob(): Job {
    val jobType = "greeting"
    val target = listOf("test")

    return jobPersistenceService.createJob(jobType, target)
  }
}
