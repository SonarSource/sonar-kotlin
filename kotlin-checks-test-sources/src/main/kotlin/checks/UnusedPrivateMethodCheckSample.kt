package checks

class UnusedPrivateMethodKotlinCheckTest {

    private fun unused() { // Noncompliant {{Remove this unused private "unused" method.}}
//              ^^^^^^
    }

    private fun used() {
        ::used
    }

    private operator fun plus(p: Int) {
        val x = fun () {} // Anonymous
    }


    private infix fun usedInfix(p: Int) = p // Compliant, used as infix function
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
