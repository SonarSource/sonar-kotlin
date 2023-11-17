package checks

import kotlinx.coroutines.CoroutineScope
import java.io.IOException
import java.net.HttpURLConnection
import java.util.Arrays
import kotlin.coroutines.CoroutineContext

class UselessNullCheckCheckSampleNoSemantics {
    fun foo() {
        val s: String = ""

        if (s == null) {} // Compliant FN (missing semantics)


        if (s != null) {} // Compliant FN (missing semantics)
        s?.doSomething() // Compliant FN (missing semantics)

        fun foo(s: Any): String {
            s ?: return "" // Compliant FN (missing semantics)

            return s.toString()
        }
        requireNotNull(s) // Compliant FN (missing semantics)


        checkNotNull(s) // Compliant FN (missing semantics)
        s!!.doSomething() // Compliant FN (missing semantics)

        null!! // Noncompliant {{Remove this useless non-null assertion `!!`, it always fails.}}
        null?.doSomething() // Noncompliant {{Remove this useless null-safe access `?.`, it always fails.}}
        null ?: doSomething() // Noncompliant
        null != "" // Noncompliant
        0 != null // Noncompliant
        doSomething() ?: null // Compliant FN (missing semantics)
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
        a!! // Compliant FN (missing semantics)

        var b: String? = null
        b!! // Compliant FN. We don't currently resolve the value of vars.

        var c: String? = null
        c = "foo"
        c!! // Compliant FN. We don't currently resolve the value of vars.

        var d: String = ""
        d!! // Compliant FN (missing semantics)

        var e = getSomething()
        e!! // Compliant FN (missing semantics)

        val f = getSomethingNullable()
        f!! // Compliant

        aField!! // Compliant FN (missing semantics)

        // The following is a compliant FN. bField is declared as:
        // val bField:String? = ""
        // so the type is technically nullable but the value is never null. We could solve this by resolving the runtime value expression.
        // However, this causes FPs in some situations, see SONARKT-373.
        bField!! // Compliant FN

        cField!! // Compliant FN (missing semantics)
        dField!!
        eField!! // Compliant FN (missing semantics)
        fField!!
        gField!!
        hField!! // Compliant FN (missing semantics)
    }

    fun `ensure we don't trigger on some unexpected code`(foo: Any) {
        val s: String = ""

        if (s == "") {}
        if (s != "") {}

        var i: Int = 0
        i++

        (foo as? String)?.isLong() ?: true // Compliant regression test (SONARKT-373)
    }

    private fun Any?.doSomething() {}
    private fun getSomething() = ""
    private fun getSomethingNullable(): String? = null
}


private fun platformTypesShouldBeCompliant() {
    val list = Arrays.asList<String>(null)
    val item = list.first()
    item.length
    item?.length
    item!!
}
private fun String.isLong() = length > 10
