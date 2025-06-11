package io.tolgee.jobs.service

import io.tolgee.jobs.entity.Job
import io.tolgee.jobs.service.queue.JobQueue
import org.springframework.stereotype.Service

@Service
class JobStartService(
  private val jobPersistenceService: JobPersistenceService,
  private val jobQueueService: JobQueue
) {
  fun runJob(): Job {
    val job = jobPersistenceService.createJob("greet", listOf("John Doe"))
    jobQueueService.add(job.id)
    return job
  }
}
