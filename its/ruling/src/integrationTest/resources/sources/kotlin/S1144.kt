// S1144 Copyright
class UnusedPrivateMethodKotlinCheckTest() {
    private val map = mutableMapOf<String, String>()

    private fun unused() {} // Noncompliant

    // Serializable method should not raise any issue in Kotlin.
    private fun writeObject() {
        // Not empty
    }
    private fun readObject() {
        // Not empty
    }
    private fun writeReplace() {
        // Not empty
    }
    private fun readResolve() {
        // Not empty
    }
    private fun readObjectNoData() {
        // Not empty
    }

    private operator fun set(index: String, value: String) { // Compliant, called by "callGet"
        map[index] = value
    }

    private operator fun get(index: String) = map[index] // Compliant, called by "callSet"

    private operator fun plus(value: UnusedPrivateMethodKotlinCheckTest):UnusedPrivateMethodKotlinCheckTest? = value // Compliant, used by "callPlus"

    private external fun notAnOperator(): String // Noncompliant

    fun callGet(index: String): String? {
        return this[index]
    }

    fun callSet(index: String, value: String) {
        this[index] = value
    }

    fun callPlus() {
        this + this
    }
}
