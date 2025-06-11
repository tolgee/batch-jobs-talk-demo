package io.tolgee.jobs.service.jobProcessors

import io.tolgee.jobs.entity.Job
import org.springframework.stereotype.Component

@Component
class NoOpJobProcessor : JobProcessor {
  override val type: String
    get() = "no-op"

  override fun process(job: Job) {
  }
}
