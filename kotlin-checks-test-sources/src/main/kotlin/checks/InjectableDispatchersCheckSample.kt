package checks

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.EmptyCoroutineContext

class InjectableDispatchersCheckSample(val injectable: CoroutineDispatcher = Dispatchers.IO) {
    suspend fun simple() {
        coroutineScope {
            launch(Dispatchers.IO) { // Noncompliant
            }

            async(Dispatchers.Default) { // Noncompliant
            }

            withContext(Dispatchers.Main) { // Noncompliant
            }

            launch(Dispatchers.Unconfined) { // Noncompliant
            }
        }
    }

    suspend fun complex() {
        val dispatcher = Dispatchers.IO // Noncompliant@+3
//                       ^^^^^^^^^^^^^^>
        coroutineScope {
            launch(dispatcher) {  }
//                 ^^^^^^^^^^
        }
    }

    suspend fun compliant() {
        coroutineScope {
            launch(injectable) {  } // Compliant
            async(injectable) {  } // Compliant
            withContext(injectable) {  } // Compliant
            launch { } // Compliant
            async { } // Compliant

            launch(EmptyCoroutineContext) {} // Compliant
        }
    }
}
