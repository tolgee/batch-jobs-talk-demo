package io.tolgee.jobs.service.jobProcessors

import io.tolgee.jobs.entity.Job
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.lang.Thread.sleep

@Component
class GreetingJobProcessor : JobProcessor {
  private val logger = LoggerFactory.getLogger(GreetingJobProcessor::class.java)

  override val type: String
    get() = "greet"

  override fun process(job: Job) {
    logger.info("Greeting...")
    sleep(500)
    logger.info("Hello, ${job.target}")
    logger.info("Greeting done...")
  }
}
