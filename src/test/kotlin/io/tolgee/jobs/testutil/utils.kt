import org.awaitility.Awaitility
import java.time.Duration
import java.util.concurrent.TimeUnit

fun waitFor(pollTimeInMs: Int = 100, timeoutInMs: Long = 10000, fn: () -> Boolean) =
  Awaitility.await()
    .pollDelay(pollTimeInMs.toLong(), TimeUnit.MILLISECONDS)
    .timeout(timeoutInMs, TimeUnit.MILLISECONDS)
    .until(fn)


fun measureTime(fn: () -> Unit): Duration {
  val start = System.currentTimeMillis()
  fn()
  return Duration.ofMillis(System.currentTimeMillis() - start)
}
