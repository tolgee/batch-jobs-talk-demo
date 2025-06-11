package io.tolgee.jobs.service

import io.tolgee.jobs.entity.Job
import io.tolgee.jobs.repository.JobRepository
import io.tolgee.jobs.util.UlidGenerator
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
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
   * Attempts to retrieve and lock a pending job by its ID without changing its status.
   * This ensures that the job is locked for the duration of the calling transaction.
   *
   * @param id The ID of the job to retrieve and lock
   * @return The locked job, or null if no job with the specified ID and PENDING status exists
   */
  @Transactional
  fun getJobWithLocking(id: String): Job? {
    val job = entityManager.createNativeQuery(
      """
      SELECT *
      FROM job
      WHERE id = :id AND status = 'PENDING'
      FOR UPDATE SKIP LOCKED
    """, Job::class.java
    )
      .setParameter("id", id)
      .resultList.singleOrNull() as Job?
    return job
  }

  fun getJob(id: String): Job {
    return jobRepository.findById(id).orElseThrow { throw Exception("Job not found") }
  }

  fun setJobSuccessful(job: Job) {
    entityManager.createNativeQuery(
      """UPDATE job
         SET status = 'SUCCEEDED'
         WHERE id = :jobId"""
    ).setParameter("jobId", job.id)
      .executeUpdate()
  }

  fun setJobFailed(job: Job) {
    entityManager.createNativeQuery(
      """UPDATE job
         SET status = 'FAILED'
         WHERE id = :jobId"""
    ).setParameter("jobId", job.id)
      .executeUpdate()
  }
}
