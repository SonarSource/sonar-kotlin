package checks

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor

const val CONST_ZERO = 0;
const val CONST_ONE = 1;

class ScheduledThreadPoolExecutorZeroCheckSample {

    fun foo(executor: ThreadPoolExecutor, scheduledExecutor: ScheduledThreadPoolExecutor): List<ThreadPoolExecutor> {
        val ZERO = 0;
        executor.corePoolSize = 0 // Noncompliant {{Increase the "corePoolSize".}}
        //                      ^
        scheduledExecutor.corePoolSize = 0 // Noncompliant {{Increase the "corePoolSize".}}
        //                               ^
        with(executor) {
            corePoolSize = CONST_ZERO // Noncompliant {{Increase the "corePoolSize".}}
            //             ^^^^^^^^^^
            corePoolSize = 3 // Compliant
        }
        var corePoolSize = 1
        corePoolSize = 0 // Compliant
        executor.corePoolSize = ZERO + 1 // Compliant
        executor.maximumPoolSize = 0
        executor.corePoolSize += 1 // Compliant
        executor?.corePoolSize = ZERO // Noncompliant
        executor.corePoolSize = executor.corePoolSize + 1 // Compliant
        executor.setCorePoolSize(CONST_ONE - 1) // Noncompliant {{Increase the "corePoolSize".}}
        //                       ^^^^^^^^^^^^^
        executor.setCorePoolSize(3) // Compliant

        (executor.corePoolSize) = 0 // Noncompliant
        (executor).corePoolSize = 0 // Noncompliant

        return listOf(
            ScheduledThreadPoolExecutor(0), // Noncompliant {{Increase the "corePoolSize".}}
            //                          ^
            ScheduledThreadPoolExecutor(2), // Compliant
            ScheduledThreadPoolExecutor(executor.corePoolSize), // Compliant
        )
    }

}
