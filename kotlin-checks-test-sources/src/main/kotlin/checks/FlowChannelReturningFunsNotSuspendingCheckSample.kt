package checks

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

abstract class FlowChannelReturningFunsNotSuspendingCheckSample {
    suspend fun noncompliantFlow1(): Flow<String>? { // Noncompliant {{Functions returning "Flow" or "Channel" should not be suspending}}
//  ^^^^^^^
//                                  <^^^^^^^^^^^^^
        return null
    }

    suspend fun noncompliantFlow2(): Flow<*> { // Noncompliant
        return flowOf(1)
    }

    suspend fun noncompliantFlow3() = flowOf("") // Noncompliant

    suspend fun noncompliantFlow4() = flowOf("") as Flow<*>? // Noncompliant

    abstract suspend fun noncompliantFlow5(): Flow<Int> // Noncompliant

    suspend fun noncompliantChannel1(): Channel<String>? { // Noncompliant
        return null
    }

    suspend fun noncompliantChannel2(): Channel<*> { // Noncompliant
        return Channel<Int> { }
    }

    suspend fun noncompliantChannel3() = Channel<String> {} // Noncompliant

    suspend fun noncompliantChannel4() = null as Flow<*>? // Noncompliant

    abstract suspend fun noncompliantChannel5(): Channel<Int> // Noncompliant


    fun compliantFlow1(): Flow<String>? { // Compliant
        return null
    }

    fun compliantFlow2(): Flow<*> { // Compliant
        return flowOf(1)
    }

    fun compliantFlow3() = flowOf("") // Compliant

    fun compliantFlow4() = flowOf("") as Flow<*>? // Compliant

    abstract fun compliantFlow5(): Flow<Int> // Compliant

    fun compliantChannel1(): Channel<String>? { // Compliant
        return null
    }

    fun compliantChannel2(): Channel<*> { // Compliant
        return Channel<Int> { }
    }

    fun compliantChannel3() = Channel<String> {} // Compliant

    fun compliantChannel4() = null as Flow<*>? // Compliant

    abstract fun compliantChannel5(): Channel<Int> // Compliant

    suspend fun foo(): String = "" // Compliant (other return type)
}
