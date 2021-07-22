package checks

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.delay

class ViewModelSuspendingFunctionsCheckSample: ViewModel() {
    
    fun function() {}
    
    suspend fun suspendingFunction() { // Noncompliant {{Classes extending "ViewModel" should not expose suspending functions.}}
//              ^^^^^^^^^^^^^^^^^^
        privateSuspendingFunction()
    }
    
    private fun privateFun() {}

    private suspend fun privateSuspendingFunction() {
        delay(500L)
    }
    
}

class NotAViewModel {
    suspend fun suspendingFunction() { // Compliant
        delay(500L)
    }  
}
