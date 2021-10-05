package sources.kotlin

import java.util.concurrent.ScheduledThreadPoolExecutor

class S2122 {
    val executor = ScheduledThreadPoolExecutor(0) // Noncompliant
}
