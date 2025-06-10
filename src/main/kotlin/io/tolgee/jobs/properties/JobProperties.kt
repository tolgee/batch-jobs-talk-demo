package io.tolgee.jobs.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "jobs")
class JobProperties {
  var perNodeConcurrency: Int = 1
  var useRedis: Boolean = false
}
