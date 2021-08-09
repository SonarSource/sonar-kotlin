package checks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class FinalFlowOperationCheckSample {
    suspend fun foo(flow: Flow<String>) {
        // Noncompliant@+1 {{Unused coroutines Flow.}}
        flow.filter { true }
//           ^^^^^^

        flow.filter { true }
            .map {  } // Noncompliant

        flow.map { }
            .map {  }
            .map {  }
            .map {  }
            .map {  } // Noncompliant@+1
            .map {  }
//           ^^^

        flow.filter { true }
            .count() // Compliant
    }

    suspend fun foo2(flowGen: () -> Flow<String>) {
        flowGen().filter { true }
            .map {  } // Noncompliant

        val x = flowGen()
            .map { it } // Compliant, as it is used later
        foo(x) // Compliant

        val lambda = {
            flowGen().filter { true }
        }

        lambda // Compliant
        lambda() // Noncompliant
        lambda()
            .map { } // Noncompliant
    }
}
