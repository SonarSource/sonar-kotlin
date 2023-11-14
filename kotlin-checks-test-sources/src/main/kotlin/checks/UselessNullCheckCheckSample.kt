package checks

class UselessNullCheckCheckSample {
    fun foo() {
        val s: String = ""

        if (s == null) {} // Noncompliant {{Remove this useless null check, it always fails.}}
//          ^^^^^^^^^

        if (s != null) {} // Noncompliant {{Remove this useless non-null check, it always succeeds.}}
        s?.doSomething() // Noncompliant {{Remove this useless null-safe access `?.`, it always succeeds.}}
//       ^^
        fun foo(s: Any): String {
            s ?: return "" // Noncompliant {{Remove this useless elvis operation `?:`, it always succeeds.}}
//            ^^
            return s.toString()
        }
        requireNotNull(s) // Noncompliant {{Remove this useless non-null check `requireNotNull`, it always succeeds.}}
//      ^^^^^^^^^^^^^^^^^

        checkNotNull(s) // Noncompliant {{Remove this useless non-null check `checkNotNull`, it always succeeds.}}
        s!!.doSomething() // Noncompliant {{Remove this useless non-null assertion `!!`, it always succeeds.}}
//       ^^
        null!! // Noncompliant {{Remove this useless non-null assertion `!!`, it always fails.}}
        null?.doSomething() // Noncompliant {{Remove this useless null-safe access `?.`, it always fails.}}
        null ?: doSomething() // Noncompliant
        doSomething() ?: null // Noncompliant
    }

    val aField: String? = null
    val bField: String? = ""
    val cField: String = ""
    val dField = getSomethingNullable()
    val eField = getSomething()

    var fField: String? = null
    var gField: String? = ""
    var hField: String = ""

    fun bar() {
        val a: String? = null
        a!! // Noncompliant

        var b: String? = null
        b!! // Compliant FN. We don't currently resolve the value of vars.

        var c: String? = null
        c = "foo"
        c!! // Compliant FN. We don't currently resolve the value of vars.

        var d: String = ""
        d!! // Noncompliant

        var e = getSomething()
        e!! // Noncompliant

        val f = getSomethingNullable()
        f!! // Compliant

        aField!! // Noncompliant
        bField!! // Noncompliant
        cField!! // Noncompliant
        dField!!
        eField!! // Noncompliant
        fField!!
        gField!!
        hField!! // Noncompliant
    }

    fun `ensure we don't trigger on some unexpected code`() {
        val s: String = ""

        if (s == "") {}
        if (s != "") {}

        var i: Int = 0
        i++


    }

    private fun Any?.doSomething() {}
    private fun getSomething() = ""
    private fun getSomethingNullable(): String? = null
}
