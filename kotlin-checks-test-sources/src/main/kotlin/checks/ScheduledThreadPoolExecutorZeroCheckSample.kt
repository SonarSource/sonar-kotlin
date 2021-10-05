package checks

import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadPoolExecutor

class ScheduledThreadPoolExecutorZeroCheckSample {

    fun foo(executor: ThreadPoolExecutor): List<ThreadPoolExecutor> {
        executor.corePoolSize = 0 // Noncompliant {{Increase the "corePoolSize".}}
        //                      ^
        with(executor) {
            corePoolSize = 0 // Noncompliant {{Increase the "corePoolSize".}}
            //             ^
            corePoolSize = 3 // Compliant
        }
        var corePoolSize = 1
        corePoolSize = 0 // Compliant
        executor.corePoolSize = 5 // Compliant
        executor.maximumPoolSize = 0
        executor.corePoolSize += 1 // Compliant
        executor?.corePoolSize = 0 // Noncompliant
        executor.corePoolSize = executor.corePoolSize + 1 // Compliant
        executor.setCorePoolSize(0) // Noncompliant {{Increase the "corePoolSize".}}
        //                       ^
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
