package io.tolgee.jobs.service

import io.tolgee.jobs.dtos.JobStatus
import io.tolgee.jobs.entity.Job
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component


@Component
class JobStatusReporter(
  private val redissonClient: RedissonClient?
) {
  fun reportJobRunning(job: Job) {
    publish(job.id, Job.Status.RUNNING)
  }

  fun reportJobSuccessful(job: Job) {
    publish(job.id, Job.Status.SUCCEEDED)
  }

  fun reportJobFailed(job: Job) {
    publish(job.id, Job.Status.FAILED)
  }

  private fun publish(jobId: String, status: Job.Status) {
    val topic = redissonClient?.getTopic("job-status") ?: return
    topic.publish(JobStatus(jobId, status))
  }
}
