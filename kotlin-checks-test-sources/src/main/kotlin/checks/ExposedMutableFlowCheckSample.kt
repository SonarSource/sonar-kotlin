package checks

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

val exposedMutableStateFlow2: MutableStateFlow<String> = MutableStateFlow("") // Noncompliant
val exposedMutableStateFlow3 = MutableStateFlow(0) // Noncompliant
lateinit var exposedMutableStateFlow4: MutableStateFlow<Boolean> // Noncompliant

val exposedMutableSharedFlow2: MutableSharedFlow<String> = MutableSharedFlow() // Noncompliant
val exposedMutableSharedFlow3 = MutableSharedFlow<Int>() // Noncompliant
lateinit var exposedMutableSharedFlow4: MutableSharedFlow<Boolean> // Noncompliant

class Noncompliant(
    val exposedMutableStateFlow0: MutableStateFlow<String>, // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    internal var exposedMutableStateFlow1: MutableStateFlow<String>, // Noncompliant
    val exposedMutableSharedFlow0: MutableSharedFlow<String>, // Noncompliant
    var exposedMutableSharedFlow1: MutableSharedFlow<String> // Noncompliant
) {
    internal val exposedMutableStateFlow2: MutableStateFlow<String> = MutableStateFlow("") // Noncompliant
    val exposedMutableStateFlow3 = MutableStateFlow(0) // Noncompliant
    lateinit var exposedMutableStateFlow4: MutableStateFlow<Boolean> // Noncompliant

    val exposedMutableSharedFlow2: MutableSharedFlow<String> = MutableSharedFlow() // Noncompliant
    internal val exposedMutableSharedFlow3 = MutableSharedFlow<Int>() // Noncompliant
    lateinit var exposedMutableSharedFlow4: MutableSharedFlow<Boolean> // Noncompliant
}

val exposedStateFlow2: StateFlow<String> = MutableStateFlow("") // Compliant
val exposedStateFlow3 = MutableStateFlow(0).asStateFlow() // Compliant
lateinit var exposedStateFlow4: StateFlow<Boolean> // Compliant

val exposedSharedFlow2: SharedFlow<String> = MutableSharedFlow() // Compliant
val exposedSharedFlow3 = MutableSharedFlow<Int>().asSharedFlow() // Compliant
lateinit var exposedSharedFlow4: SharedFlow<Boolean> // Compliant

class Compliant(
    val exposedStateFlow0: StateFlow<String>, // Compliant
    var exposedStateFlow1: StateFlow<String>, // Compliant
    val exposedSharedFlow0: SharedFlow<String>, // Compliant
    var exposedSharedFlow1: SharedFlow<String>, // Compliant
    private val notExposedMutableStateFlow0: MutableStateFlow<String>, // Compliant
    protected var notExposedMutableStateFlow1: MutableStateFlow<String>, // Compliant
    protected val notExposedMutableSharedFlow0: MutableSharedFlow<String>, // Compliant
    private var notExposedMutableSharedFlow1: MutableSharedFlow<String> // Compliant
) {

    val exposedStateFlow2: StateFlow<String> = MutableStateFlow("") // Compliant
    val exposedStateFlow3 = MutableStateFlow(0).asStateFlow() // Compliant
    lateinit var exposedStateFlow4: StateFlow<Boolean> // Compliant

    val exposedSharedFlow2: SharedFlow<String> = MutableSharedFlow() // Compliant
    val exposedSharedFlow3 = MutableSharedFlow<Int>().asSharedFlow() // Compliant
    lateinit var exposedSharedFlow4: SharedFlow<Boolean> // Compliant

    private val notExposedMutableStateFlow2: MutableStateFlow<String> = MutableStateFlow("") // Compliant
    private val notExposedMutableStateFlow3 = MutableStateFlow(0) // Compliant
    protected lateinit var notExposedMutableStateFlow4: MutableStateFlow<Boolean> // Compliant

    protected val notExposedMutableSharedFlow2: MutableSharedFlow<String> = MutableSharedFlow() // Compliant
    private val notExposedMutableSharedFlow3 = MutableSharedFlow<Int>() // Compliant
    private lateinit var notExposedMutableSharedFlow4: MutableSharedFlow<Boolean> // Compliant

    fun foo(
        exposedMutableStateFlow0: MutableStateFlow<String>, // Compliant (local parameter)
        exposedMutableSharedFlow0: MutableSharedFlow<String>, // Compliant (local parameter)
    ) {
        val exposedMutableStateFlow2: MutableStateFlow<String> = MutableStateFlow("") // Compliant (local val/var in method)
        val exposedMutableStateFlow3 = MutableStateFlow(0) // Compliant (local val/var in method)
        lateinit var exposedMutableStateFlow4: MutableStateFlow<Boolean> // Compliant (local val/var in method)

        val exposedMutableSharedFlow2: MutableSharedFlow<String> = MutableSharedFlow() // Compliant (local val/var in method)
        val exposedMutableSharedFlow3 = MutableSharedFlow<Int>() // Compliant (local val/var in method)
        lateinit var exposedMutableSharedFlow4: MutableSharedFlow<Boolean> // Compliant (local val/var in method)
    }
}

class AlsoCompliant {
    constructor(secondaryConstructorMutableFlow: MutableStateFlow<String>) { // Compliant
    }
}
