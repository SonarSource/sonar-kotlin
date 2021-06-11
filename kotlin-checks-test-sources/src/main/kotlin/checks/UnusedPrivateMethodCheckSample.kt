package checks

class UnusedPrivateMethodKotlinCheckTest {

    private fun unused() { // Noncompliant
    }

    private fun used() {
        ::used
    }

    private operator fun plus(p: Int) {
    }

    fun publicUnusedFun() {
    }

    // TODO false-negative
    private fun String.unusedExtension() {
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
