package checks

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class FinalFlowOperationCheckSample {
    suspend fun foo(flow: Flow<String>) {
        flow.filter { true } // Noncompliant

        flow.filter { true }
            .map {  } // Noncompliant

        flow.map { }
            .map {  }
            .map {  }
            .map {  }
            .map {  }
            .map {  } // Noncompliant

        flow.filter { true }
            .count() // Compliant
    }
}
