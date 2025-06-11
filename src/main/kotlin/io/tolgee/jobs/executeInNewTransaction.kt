package io.tolgee.jobs

import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionTemplate

fun <T> executeInNewTransaction(
  transactionManager: PlatformTransactionManager,
  fn: (ts: TransactionStatus) -> T,
): T {
  val tt = TransactionTemplate(transactionManager)
  tt.propagationBehavior = TransactionTemplate.PROPAGATION_REQUIRES_NEW
  @Suppress("UNCHECKED_CAST")
  return tt.execute { ts ->
    fn(ts)
  } as T
}
