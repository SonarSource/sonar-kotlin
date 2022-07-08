package checks

import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class CoroutinesTimeoutApiUnusedCheckSample {
    suspend fun foo() {
        coroutineScope {
            println("")
            // Noncompliant@+8
            val job = launch {
//                    ^^^^^^>
                delay(2000L)
                println("Finished")
            }
            delay(500L)
//          ^^^^^>
            job.cancel()
//              ^^^^^^
        }

        coroutineScope {
            println("")
            val deferred = async() {
                delay(2000L)
                println("Finished")
            }
            delay(500L)
            deferred.cancel() // Noncompliant
        }

        coroutineScope {
            val deferred = async {
                delay(2000L)
                println("Finished")
                100
            }
            delay(500L)
            deferred.cancel() // Noncompliant
            deferred.await()
        }

        coroutineScope {
            println("")
            val i = withTimeoutOrNull(500L) {
                delay(2000L)
                println("Finished")
                100
            }
            i
        }

        coroutineScope {
            println("")
            val job = launch {
                delay(2000L)
                println("Finished")
            }
            println()
            delay(500L)
            job.cancel() // Compliant FP - we only consider the simplest case, i.e. no statements between job creation, delay & cancel
        }

        coroutineScope {
            println("")
            val job = launch {
                delay(2000L)
                println("Finished")
            }
            val foo = launch {

            }
            println()
            delay(500L)
            job.cancel() // Compliant FP - we only consider the simplest case, i.e. no statements between job creation, delay & cancel
        }

        coroutineScope {
            val job = Job()
            delay(500L)
            job.cancel() // Compliant, not using launch or async
        }

        coroutineScope {
            val job1 = Job()
            val job = job1
            delay(500L)
            job.cancel() // Compliant, not using launch or async
        }

        coroutineScope {
            with(Job()) {
                cancel()
            }
        }

        coroutineScope {
            println("")
            withTimeout(500) {
                delay(2000L)
                println("Finished")
            }
        }

        coroutineScope {
            println("")
            withTimeoutOrNull(500) {
                delay(2000L)
                println("Finished")
            }
        }

        coroutineScope {
            Job().cancel()
        }
    }
}

class JobNotFollowedByDelay {
    private val job = Job() // 'Job' not followed by 'delay' should not crash

    fun justCancel() {
        job.cancel()
    }
}
