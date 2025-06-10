package io.tolgee.jobs.entity

import io.tolgee.jobs.util.UlidGenerator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

/**
 * Entity representing a job in the system.
 */
@Entity
@Table(name = "job")
class Job(
  /**
   * Unique identifier for the job.
   * Format: "job_<ulid>"
   */
  @Id
  val id: String = UlidGenerator.generateJobId(),

  val jobType: String,

  /**
   * Target data for the job stored as JSONB.
   */
  @Column(nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  val target: List<Any>,

  @Enumerated(EnumType.STRING)
  var status: Status = Status.PENDING
) {
  enum class Status {
    PENDING, RUNNING, FAILED, SUCCEEDED
  }
}
