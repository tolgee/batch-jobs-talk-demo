package io.tolgee.jobs.service.queue

import io.tolgee.jobs.properties.JobProperties
import org.redisson.api.RedissonClient
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

@Component
class QueueProvider(
  private val jobProperties: JobProperties,
  private val applicationContext: ApplicationContext
) {

  val logger = org.slf4j.LoggerFactory.getLogger(QueueProvider::class.java)
  fun provide(): BlockingQueue<String> {
    if (jobProperties.useRedis) {
      logger.info("Using Redis for jobs queue")
      return redissonClient.getBlockingQueue("jobQueue")
    }
    logger.info("Using in-memory queue for jobs")
    return LinkedBlockingQueue()
  }

  private val redissonClient by lazy {
    applicationContext.getBean(RedissonClient::class.java)
  }
}
