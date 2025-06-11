package io.tolgee.jobs.service.queue

import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue

@Component
class JobQueue(
  private val queueProvider: QueueProvider
) {
  private val queue: BlockingQueue<String> by lazy {
    queueProvider.provide()
  }

  fun take(): String? {
    return queue.take()
  }

  fun add(jobId: String) {
    queue.add(jobId)
  }
}
