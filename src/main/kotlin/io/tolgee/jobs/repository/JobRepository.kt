package io.tolgee.jobs.repository

import io.tolgee.jobs.entity.Job
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

/**
 * Repository for accessing and manipulating Job entities in the database.
 * Extends JpaRepository to provide CRUD operations and custom query methods
 * for Job entities.
 */
@Repository
interface JobRepository : JpaRepository<Job, String> {


  /**
   * Attempts to acquire and start processing a pending job using row-level locking.
   * This method uses SELECT FOR UPDATE SKIP LOCKED to ensure concurrent safety
   * when multiple instances try to process jobs simultaneously.
   *
   * @param id The ID of the job to process
   * @return The acquired Job entity with updated status, or null if no job was acquired
   */
  @Modifying
  @Query(
    value = """
    UPDATE job
    SET status = 'RUNNING'
    WHERE id = (
      SELECT id
      FROM   job
      WHERE  status = 'PENDING'
      ORDER  BY created_at
      LIMIT  1
      FOR UPDATE SKIP LOCKED
    )
    RETURNING *;
    """,
    nativeQuery = true
  )
  fun getJobWithLocking(id: String): Job?
}
