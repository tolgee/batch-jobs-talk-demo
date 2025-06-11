package io.tolgee.jobs.service.jobProcessors

import io.tolgee.jobs.entity.Job

interface JobProcessor {
  val type: String
  fun process(job: Job)
}
