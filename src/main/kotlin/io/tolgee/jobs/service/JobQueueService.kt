package io.tolgee.jobs.service

import io.tolgee.jobs.entity.Job
import org.springframework.stereotype.Service
import java.util.concurrent.LinkedBlockingQueue

@Service
class JobQueueService {
  private val queue: LinkedBlockingQueue<String> by lazy {
    LinkedBlockingQueue()
  }

  fun take(): String? {
    return queue.poll()
  }

  fun add(jobId: String) {
    queue.add(jobId)
  }
}
