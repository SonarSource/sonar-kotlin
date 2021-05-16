package checks

class AllBranchesIdenticalCheckSample {

    fun f(x: Boolean, y: Boolean, z: Int, any: Any) {
        if (x) {
            foo()
        }
        if (x) { foo() } else { bar() }

        if (x) { // Noncompliant {{Remove this conditional structure or edit its code blocks so that they're not all the same.}}
            foo()
        } else {
            foo()
        }

        if (x) foo() // Noncompliant {{Remove this conditional structure or edit its code blocks so that they're not all the same.}}
        else foo()

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

        // TODO false-positive
        when (any) { // Noncompliant
            is String -> any.foo()
            else -> any.foo()
        }

    }

    private fun String.foo() = Unit
    private fun Any?.foo() = Unit

    private fun foo() = Unit
    private fun bar() = Unit
}
