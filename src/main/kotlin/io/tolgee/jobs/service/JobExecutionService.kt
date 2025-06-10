package io.tolgee.jobs.service

import io.tolgee.jobs.entity.Job
import io.tolgee.jobs.executeInNewTransaction
import io.tolgee.jobs.properties.JobProperties
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import java.lang.Thread.sleep

@Service
open class JobExecutionService(
  private val queueService: JobQueueService,
  private val jobPersistenceService: JobPersistenceService,
  private val transactionManager: PlatformTransactionManager,
  private val jobsProperties: JobProperties
) {
  private val logger = LoggerFactory.getLogger(JobExecutionService::class.java)

  private var run = true

  @OptIn(DelicateCoroutinesApi::class)
  fun run(concurrency: Int = jobsProperties.perNodeConcurrency) = GlobalScope.launch {
    run = true
    GlobalScope.launch {
      repeat(concurrency) { workerId ->
        launch(Dispatchers.IO) {
          logger.info("Starting worker thread #$workerId")
          while (run) {
            processNextJob()
          }
        }
      }
    }
  }

  fun stop() {
    run = false
  }

  private fun processNextJob() {
    val job = getNextJob() ?: return
    executeInNewTransaction(transactionManager) {
      processJob(job)
      jobPersistenceService.setJobSuccessful(job)
    }
  }

  fun getNextJob(): Job? {
    val id = queueService.take() ?: return null
    val job = jobPersistenceService.getJobWithLocking(id)
    return job
  }

  open fun processJob(job: Job) {
    // this is only educational implementation,
    // normally we would create a separate class per job type for complex handling
    when (job.jobType) {
      "greet" -> {
        logger.info("Greeting...")
        sleep(500)
        logger.info("Hello, ${job.target}")
        logger.info("Greeting done...")
      }

      "no-op" -> {}
      else -> throw RuntimeException("Unknown job type: ${job.jobType}")
    }
  }
}
