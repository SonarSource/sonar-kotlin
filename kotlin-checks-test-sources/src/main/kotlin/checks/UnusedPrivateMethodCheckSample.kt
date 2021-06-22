package checks

class UnusedPrivateMethodKotlinCheckTest {

    private fun unused() { // Noncompliant {{Remove this unused private "unused" method.}}
//              ^^^^^^
    }

    private fun used() {
        ::used
    }

    private operator fun plus(p: Int) {
    }

    // TODO false-positive
    private infix fun usedInfix(p: Int) = p // Noncompliant
//                    ^^^^^^^^^
    init {
        this usedInfix 1
    }

    fun publicUnusedFun() {
    }

    private fun String.unusedExtension() { // Noncompliant {{Remove this unused private "unusedExtension" method.}}
    }

    // Serializable method should not raise any issue in Kotlin.
    private fun writeObject() { } // Compliant
    private fun readObject() { } // Compliant
    private fun writeReplace() { } // Compliant
    private fun readResolve() { } // Compliant
    private fun readObjectNoData() { } // Compliant

    class Inner {
        private fun unused() { // Noncompliant
        }
    }

}
