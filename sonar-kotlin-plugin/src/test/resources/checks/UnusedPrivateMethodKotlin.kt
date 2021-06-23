class UnusedPrivateMethodKotlinCheckTest() {
    private val map = mutableMapOf<String, String>()

    private fun unused() {} // Noncompliant
    private fun used() {} // Compliant
    fun callUsed() {
        used();
    }

    // Serializable method should not raise any issue in Kotlin.
    private fun writeObject() {}
    private fun readObject() {}
    private fun writeReplace() {}
    private fun readResolve() {}
    private fun readObjectNoData() {}

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
