package checks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

class StructuredConcurrencyPrinciplesCheckSample {

    @OptIn(DelicateCoroutinesApi::class)
    fun explicitlyOptIn() {
        GlobalScope.launch { } // Compliant
    }

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    fun explicitlyOptIn1() {
        GlobalScope.launch { } // Compliant
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    fun explicitlyOptIn2() {
        GlobalScope.launch { } // Compliant
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun explicitlyOptIn3() {
        GlobalScope.launch { } // Noncompliant {{Using "GlobalScope" here leads to the breaking of structured concurrency principles.}}
//      ^^^^^^^^^^^         
    }

    @DelicateCoroutinesApi
    fun explicitlyOptIn4() {
        GlobalScope.launch { } // Compliant
    }

    @kotlinx.coroutines.DelicateCoroutinesApi
    fun explicitlyOptIn5() {
        GlobalScope.launch { } // Compliant
    }

    @checks.DelicateCoroutinesApi
    fun explicitlyOptIn6() {
        GlobalScope.launch { } // Noncompliant
    }

    fun globalScopeWithoutOptIn() {
        GlobalScope.launch { } // Noncompliant {{Using "GlobalScope" here leads to the breaking of structured concurrency principles.}}
        GlobalScope.async { } // Noncompliant {{Using "GlobalScope" here leads to the breaking of structured concurrency principles.}}
    }

    suspend fun passNewJob(job: Job) {
        val coroutineScope = CoroutineScope(job)
        coroutineScope.launch(Job()) { } // Noncompliant {{Using "Job()" here leads to the breaking of structured concurrency principles.}}
//                            ^^^^^         
        coroutineScope.async(Job()) { } // Noncompliant {{Using "Job()" here leads to the breaking of structured concurrency principles.}}
//                           ^^^^^         
        withContext(Job()) { } // Noncompliant
        
        coroutineScope {
            launch(SupervisorJob()) { } // Noncompliant {{Using "SupervisorJob()" here leads to the breaking of structured concurrency principles.}}
//                 ^^^^^^^^^^^^^^^         
            async(SupervisorJob()) { } // Noncompliant
            withContext(SupervisorJob()) { } // Noncompliant {{Using "SupervisorJob()" here leads to the breaking of structured concurrency principles.}}
//                      ^^^^^^^^^^^^^^^         
        }
    }
    
    suspend fun worker() {
        coroutineScope {
            launch { } // Compliant
        }
    }

    suspend fun startLongRunningBackgroundJob(job: Job) {
        val coroutineScope = CoroutineScope(job)
        coroutineScope.launch { } // Compliant
        coroutineScope.async { } // Compliant
        supervisorScope {
            async { } // Compliant
            launch { } // Compliant
        }
    }
}

annotation class DelicateCoroutinesApi
