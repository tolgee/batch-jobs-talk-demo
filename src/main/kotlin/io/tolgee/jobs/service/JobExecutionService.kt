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
  private val queueService: JobQueue,
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
      val savePoint = savePointManager.setSavepoint()
      jobStatusReporter.reportJobRunning(job)
      try {
        processJob(job)
        jobStatusReporter.reportJobSuccessful(job)
        jobPersistenceService.setJobSuccessful(job)
      } catch (e: Exception) {
        logger.error("Job ${job.id} failed", e)
        savePointManager.rollbackSavepoint(savePoint)
        jobPersistenceService.setJobFailed(job)
        jobStatusReporter.reportJobFailed(job)
      }
    }
  }

  private fun getNextJob(): Job? {
    val id = queueService.take() ?: return null
    val job = jobPersistenceService.getJobWithLocking(id)
    return job
  }

  private fun processJob(job: Job) {
    jobProcessorMap[job.jobType]?.process(job)
      ?: throw RuntimeException("No processor for type ${job.jobType}")
  }

  private val jobProcessorMap: Map<String, JobProcessor> by lazy {
    jobProcessors.associateBy { it.type }
  }
}
