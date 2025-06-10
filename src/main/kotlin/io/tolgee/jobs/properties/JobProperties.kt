package io.tolgee.jobs.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "tolgee.jobs")
class JobProperties {
  val perNodeConcurrency: Int = 1
}
