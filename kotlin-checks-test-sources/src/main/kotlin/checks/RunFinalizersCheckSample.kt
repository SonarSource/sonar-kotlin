package java.lang

class Runtime {
    // simulate "runFinalizersOnExit" (deleted since java 11)
    fun runFinalizersOnExit(value: kotlin.Boolean): Unit {}
    fun f(): Unit = runFinalizersOnExit(true) // Noncompliant {{Remove this call to "runFinalizersOnExit()".}}
    //              ^^^^^^^^^^^^^^^^^^^
}

class System {
    // simulate "runFinalizersOnExit" (deleted since java 11)
    fun runFinalizersOnExit(value: kotlin.Boolean): Unit {}
    fun f(): Unit = runFinalizersOnExit(true) // Noncompliant {{Remove this call to "runFinalizersOnExit()".}}
    //              ^^^^^^^^^^^^^^^^^^^
}
