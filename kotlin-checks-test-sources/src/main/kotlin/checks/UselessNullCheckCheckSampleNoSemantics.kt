package checks

import kotlinx.coroutines.CoroutineScope
import java.io.IOException
import java.net.HttpURLConnection
import java.util.Arrays
import java.util.WeakHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.coroutines.CoroutineContext

class UselessNullCheckCheckSampleNoSemantics {
    fun foo() {
        val s: String = ""

        if (s == null) {} // Noncompliant


        if (s != null) {} // Noncompliant
        s?.doSomething() // Noncompliant

        fun foo(s: Any): String {
            s ?: return "" // Noncompliant

            return s.toString()
        }
        requireNotNull(s) // Compliant FN (missing semantics)


        checkNotNull(s) // Compliant FN (missing semantics)
        s!!.doSomething() // Noncompliant

        null!! // Noncompliant
        null?.doSomething() // Noncompliant
        null ?: doSomething() // Noncompliant
        null != "" // Noncompliant
        0 != null // Noncompliant
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
        c!! // Noncompliant

        var d: String = ""
        d!! // Noncompliant

        var e = getSomething()
        e!! // Noncompliant

        val f = getSomethingNullable()
        f!! // Compliant

        aField!! // Noncompliant

        // The following is a compliant FN. bField is declared as:
        // val bField:String? = ""
        // so the type is technically nullable but the value is never null. We could solve this by resolving the runtime value expression.
        // However, this causes FPs in some situations, see SONARKT-373.
        bField!! // Compliant FN

        cField!! // Noncompliant
        dField!!
        eField!! // Noncompliant
        fField!!
        gField!!
        hField!! // Noncompliant
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


private val cacheLock = ReentrantReadWriteLock()
private val exceptionCtors: WeakHashMap<Class<out Throwable>, Ctor> = WeakHashMap()
private typealias Ctor = (Throwable) -> Throwable?

private fun <E : Throwable> moo(exception: E) =
    cacheLock.read { exceptionCtors[exception.javaClass] }?.let { cachedCtor ->
        cachedCtor(exception) as E?
    }
