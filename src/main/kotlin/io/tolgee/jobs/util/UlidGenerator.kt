package io.tolgee.jobs.util

import com.github.f4b6a3.ulid.UlidCreator

/**
 * Utility class for generating ULID identifiers with prefixes.
 */
object UlidGenerator {

    private const val JOB_PREFIX = "job_"

    /**
     * Generates a ULID with the job prefix.
     * @return A string in the format "job_<ulid>"
     */
    fun generateJobId(): String {
        return JOB_PREFIX + UlidCreator.getUlid().toString()
    }
}
