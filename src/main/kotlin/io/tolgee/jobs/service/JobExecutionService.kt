package io.tolgee.jobs.service

import io.tolgee.jobs.entity.Job
import io.tolgee.jobs.executeInNewTransaction
import io.tolgee.jobs.properties.JobProperties
import io.tolgee.jobs.service.jobProcessors.JobProcessor
import io.tolgee.jobs.service.queue.JobQueue
import io.tolgee.jobs.util.SavePointManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager

@Service
class JobExecutionService(
  private val jobQueue: JobQueue,
  private val jobPersistenceService: JobPersistenceService,
  private val transactionManager: PlatformTransactionManager,
  private val jobsProperties: JobProperties,
  private val jobProcessors: List<JobProcessor>,
  private val savePointManager: SavePointManager,
  private val jobStatusReporter: JobStatusReporter
) {
  private val logger = LoggerFactory.getLogger(JobExecutionService::class.java)

  private var run = true

  @OptIn(DelicateCoroutinesApi::class)
  fun run(concurrency: Int = jobsProperties.perNodeConcurrency): kotlinx.coroutines.Job {
    run = true
    return GlobalScope.launch {
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
    // Get the next job ID from the queue, return if none available
    val nextJobId = jobQueue.take() ?: return
    executeInNewTransaction(transactionManager) {
      // Try to lock and retrieve the job, return if not found or already locked
      val job = jobPersistenceService.getJobWithLocking(nextJobId) ?: return@executeInNewTransaction
      // Create a savepoint to allow rollback if job fails
      val savePoint = savePointManager.setSavepoint()
      // Report that job execution has started
      jobStatusReporter.reportJobRunning(job)
      try {
        // Execute the job using appropriate processor
        processJob(job)
        // Report and persist successful job completion
        jobStatusReporter.reportJobSuccessful(job)
        jobPersistenceService.setJobSuccessful(job)
      } catch (e: Exception) {
        // Log error and report job failure
        logger.error("Job ${job.id} failed", e)
        // Roll back to savepoint to undo any partial changes
        savePointManager.rollbackSavepoint(savePoint)
        // Update job status to failed
        jobPersistenceService.setJobFailed(job)
        jobStatusReporter.reportJobFailed(job)
      }
    }
  }

  private fun processJob(job: Job) {
    jobProcessorMap[job.jobType]?.process(job)
      ?: throw RuntimeException("No processor for type ${job.jobType}")
  }

  private val jobProcessorMap: Map<String, JobProcessor> by lazy {
    jobProcessors.associateBy { it.type }
  }
}
