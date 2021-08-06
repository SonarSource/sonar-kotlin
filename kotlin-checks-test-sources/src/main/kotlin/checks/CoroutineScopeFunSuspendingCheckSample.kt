package checks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlin.coroutines.CoroutineContext

abstract class CoroutineScopeFunSuspendingCheckSample {
    suspend fun CoroutineScope.noncompliant1() {} // Noncompliant {{Extension functions on CoroutineScope should not be suspending.}}
//  ^^^^^^^
//             <^^^^^^^^^^^^^^

    abstract suspend fun CoroutineScope.noncompliant2() // Noncompliant

    suspend fun GlobalScope.noncompliant3() {} // Noncompliant

    suspend fun Child4.noncompliant4() {} // Noncompliant

    suspend fun noExtension() {} // Compliant

    fun CoroutineScope.compliant() {} // Compliant

    suspend fun String.compliant() {} // Compliant
}

abstract class Child1 : CoroutineScope
abstract class Child2 : Child1()
abstract class Child3 : Child2()
class Child4(override val coroutineContext: CoroutineContext) : Child3()
