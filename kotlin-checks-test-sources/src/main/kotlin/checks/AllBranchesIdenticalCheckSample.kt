package checks

sealed interface X

data object A : X
data class B(val s: String) : X
data class C(val s: String) : X

fun delegate(s: String) = s + "foo"

fun delegatesWithExtractedData(value: X) = when (value) {
    A -> ""
    is B -> delegate(value.s) // Compliant
    is C -> delegate(value.s)
}

class AllBranchesIdenticalCheckSample {

    fun f(x: Boolean, y: Boolean, z: Int, any: Any) {
        if (x) {
            foo()
        }
        if (x) { foo() } else { bar() }

        if (x) {
            foo()
        } else {
            foo()
        } // Compliant

        if (x) foo()
        else foo() // Compliant

        if (x) { foo() } else if (y) { foo() };

        if (x) { foo() } else if (y) { foo() } else { bar() };

        if (x) { // Noncompliant
            foo()
        } else if (y) {
            foo()
        } else {
            foo()
        }

        if (x) {
            bar()
        } else if (y) { // Compliant
            foo()
        } else {
            foo()
        }

        if (x) if (y) return foo() else return foo() else return bar() // Noncompliant
//             ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

        val a = 10
        val b = 20

        when (z) { }
        when (z) { 1 -> a; }
        // TODO false-positive
        when (z) { else -> b; } // Noncompliant
        when (z) { 1 -> a; else -> b; }
        when (z) { 1 -> a; else -> a; } // Noncompliant

        when (z) { 1 -> a; 2 -> a; else -> b; }
        when (z) { 1 -> a; 2 -> a; else -> a; } // Noncompliant

        when (any) {
            is String -> any.foo()
            else -> any.foo()
        } // Compliant

        if (true)
            else false

        if (true)
            else if (false)
                else false
    }

    private fun String.foo() = Unit
    private fun Any?.foo() = Unit

    private fun foo() = Unit
    private fun bar() = Unit
}
