package checks

import kotlinx.coroutines.CoroutineScope
import java.io.IOException
import java.net.HttpURLConnection
import java.util.Arrays
import java.util.WeakHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.coroutines.CoroutineContext

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
        null != "" // Noncompliant
        0 != null // Noncompliant
        doSomething() ?: null // Noncompliant
        42!! // Noncompliant
        42 != null // Noncompliant
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

        if (s == "") {
        }
        if (s != "") {
        }

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
private val exceptionCtors: WeakHashMap<Class<out Throwable>, Ctor2> = WeakHashMap()
private typealias Ctor2 = (Throwable) -> Throwable?

private fun <E : Throwable> moo(exception: E) =
    cacheLock.read { exceptionCtors[exception.javaClass] }?.let { cachedCtor ->
        cachedCtor(exception) as E?
    }

private class FooBar(
    something: Any
) {
    private val someString: String? = something as? String
    val isString: Boolean
        get() = someString != null // Compliant
}

private fun <T> testParametrised(list: List<T>): Int? {
    return list.first()?.hashCode()
}
