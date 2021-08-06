package checks

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.delay

class SuspendingFunCallerDispatcherCheckSample(val injectable: CoroutineDispatcher = Dispatchers.IO) {
    suspend fun simple() {
        coroutineScope {
            launch(Dispatchers.IO) { // Noncompliant {{Remove this dispatcher. It is pointless when used with only suspending functions.}}
//                 ^^^^^^^^^^^^^^
                delay(100L)
            }

            async(Dispatchers.Default) { // Noncompliant
                delay(100L)
                complex()
                delay(500L)
            }

            async(injectable) { // Noncompliant
//                ^^^^^^^^^^
                delay(100L)
                complex()
                delay(500L)
            }

            withContext(Dispatchers.Main) { // Compliant, async is not a suspending function
                delay(100L)
                async { "" }.await()
            }

            launch(Dispatchers.Unconfined) { // Compliant, no suspending functions
            }

            launch(Dispatchers.IO) { // Compliant
                Thread.sleep(500L)
            }
        }
    }

    suspend fun complex() {
        val dispatcher = Dispatchers.IO
        coroutineScope {
            launch(dispatcher) { // Noncompliant
                delay(100L)
                complex()
                delay(500L)
            }
        }
    }
}
