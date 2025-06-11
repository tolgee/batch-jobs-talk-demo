package io.tolgee.jobs.dtos

import io.tolgee.jobs.entity.Job

data class JobStatus(val id: String, val status: Job.Status)
