package io.tolgee.jobs.service

import io.tolgee.jobs.entity.Job
import io.tolgee.jobs.repository.JobRepository
import io.tolgee.jobs.testutil.PostgresExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest
@ExtendWith(PostgresExtension::class)
class JobPersistenceServiceTest {

  companion object {
    @DynamicPropertySource
    @JvmStatic
    fun setProperties(registry: DynamicPropertyRegistry) {
      PostgresExtension.registerPgProps(registry)
    }
  }

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
    val jobId = createJob().id

    val lockedJob = jobPersistenceService.getJobWithLocking(jobId)

    assertThat(lockedJob).isNotNull
    assertThat(lockedJob?.id).isEqualTo(jobId)
    assertThat(lockedJob?.status).isEqualTo(Job.Status.RUNNING)
  }

  private fun createJob(): Job {
    val jobType = "greeting"
    val target = listOf("test")

    return jobPersistenceService.createJob(jobType, target)
  }
}
