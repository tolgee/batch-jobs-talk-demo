package io.tolgee.jobs.service

import io.tolgee.jobs.entity.Job
import io.tolgee.jobs.repository.JobRepository
import io.tolgee.jobs.util.UlidGenerator
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Service for persisting and retrieving jobs.
 */
@Service
class JobPersistenceService(
  private val jobRepository: JobRepository,
  private val entityManager: EntityManager
) {

  /**
   * Creates a new job with the specified type and target.
   *
   * @param jobType The type of the job
   * @param target The target data for the job
   * @return The created job
   */
  @Transactional
  fun createJob(jobType: String, target: List<Any>): Job {
    val jobId = UlidGenerator.generateJobId()
    val job = Job(
      id = jobId,
      jobType = jobType,
      target = target,
    )
    entityManager.persist(job)
    return job
  }

  /**
   * Attempts to retrieve a job by its ID and updates its status to RUNNING within a new transaction.
   * This ensures that the job is locked for update, allowing safe processing in a concurrent environment.
   *
   * @param id The ID of the job to retrieve and lock
   * @return The job with its status updated to RUNNING, or null if no job with the specified ID exists
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun getJobWithLocking(id: String): Job? {
    val job = entityManager.createNativeQuery(
      """UPDATE job
    SET status = 'RUNNING'
    WHERE id = (
      SELECT id
      FROM   job
      WHERE  status = 'PENDING'
      ORDER  BY id
      LIMIT  1
      FOR UPDATE SKIP LOCKED
    )
    RETURNING *;""", Job::class.java
    ).singleResult as Job
    return job
  }

  fun getJob(id: String): Job {
    return jobRepository.findById(id).orElseThrow { throw Exception("Job not found") }
  }

  fun save(job: Job) {
    jobRepository.save(job)
  }

  fun setJobSuccessful(job: Job) {
    entityManager.createNativeQuery(
      """UPDATE job
         SET status = 'SUCCEEDED'
         WHERE id = :jobId"""
    ).setParameter("jobId", job.id)
      .executeUpdate()
  }
}
