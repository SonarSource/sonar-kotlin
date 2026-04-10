package checks

import otherpackage.get
import java.util.Calendar
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class IndexedAccessCheckSampleNoSemantics {

    fun withoutIndexedAccessors(
        list: MutableList<Int>,
        map: MutableMap<String, Int>,
        grid: Grid2,
        value: Any,
        cal: Calendar,
        future: CompletableFuture<Int>,
    ) {
        list.get(1) // Noncompliant {{Replace function call with indexed accessor.}}
//           ^^^
        list.set(1, 42) // Noncompliant {{Replace function call with indexed accessor.}}
//           ^^^
        map.get("b") // Noncompliant {{Replace function call with indexed accessor.}}
        map.set("b", 42) // FN, MutableMap.set is an extension function not available without classpath
        grid.get(1, 2) // Noncompliant {{Replace function call with indexed accessor.}}
        grid.set(1, 2, 42) // Noncompliant {{Replace function call with indexed accessor.}}
        value.get(42) // FN, Any has no get operator, only exposed via an extension function in Importable.kt, function in , can't resolve without semantics
        // Java get/set methods can be replaced with indexed access operators via Java-Interop (https://kotlinlang.org/docs/java-interop.html#operators)
        cal.get(Calendar.YEAR) // Noncompliant {{Replace function call with indexed accessor.}}
        // FP without semantics: incorrect resolution due to singleFunctionCallOrNull
        future.get(1L, TimeUnit.SECONDS) // Noncompliant {{Replace function call with indexed accessor.}}
    }

    fun withIndexedAccessors(
        lisp: Lisp2<Int>,
        maybeNullList: MutableList<Int>?,
        list: MutableList<Int>,
        map: MutableMap<String, Int>,
        grid: Grid2,
        num: AtomicInteger,
        root: GenericAccessorClass2,
    ) {
        num.get() // Compliant, class doesn't have an index access operator
        lisp.get(1) // Compliant, not an operator
        list[1] // Compliant
        list[1] = 42 // Compliant
        map["b"] // Compliant
        map["b"] = 42 // Compliant
        grid[1, 2] // Compliant
        grid[1, 2] = 42 // Compliant
        list.getOrNull(2) // Complaint, not an operator
        list.getOrElse(3) { 42 } // Complaint, not an operator
        map.getValue("a") // Complaint, not an operator
        map.getOrElse("c") { 42 } // Complaint, not an operator
        root.get<String>("id") // Compliant: explicit type parameter cannot be expressed with [] syntax
        maybeNullList?.get(0) // Compliant: safe call uses KtSafeQualifiedExpression, not KtDotQualifiedExpression
    }
}

interface Grid2 {
    operator fun get(row: Int, column: Int): Int
    operator fun set(row: Int, column: Int, value: Int)
}

interface Lisp2<T> {
    fun get(index: Int)
}

open class ParentClass2 {
    private val _array = mutableListOf<String>()

    open operator fun get(index: Int): String {
        return _array[index]
    }

    open operator fun set(index: Int, value: String) {
        _array[index] = value
    }

    val size: Int get() = _array.size
}

class ChildClass2 : ParentClass() {

    override operator fun get(index: Int): String {
        return super.get(index) // Compliant, because `super[index]` does not compile
    }

    override operator fun set(index: Int, value: String) {
        super.set(index, value) // Compliant, because `super[index]` does not compile
    }

    fun getOrEmpty(index: Int): String =
        if (index in 0 until size)
            get(index) // Compliant, because `[index]` does not compile and `this[index]` is no simplification
        else
            ""

    fun setIfExist(index: Int, value: String) {
        if (index in 0 until size) {
            set(index, value) // Compliant, because `[index]` does not compile and `this[index]` is no simplification
        }
    }

    fun getOrEmptyAlternate(index: Int): String =
        if (index in 0 until size)
            this.get(index) // Noncompliant {{Replace function call with indexed accessor.}}
        else
            ""

    fun setIfExistAlternate(index: Int, value: String) {
        if (index in 0 until size) {
            this.set(index, value) // Noncompliant {{Replace function call with indexed accessor.}}
        }
    }

    inner class InnerClass {
        operator fun get(index: Int): String {
            return this@ChildClass2.get(index) // Noncompliant {{Replace function call with indexed accessor.}}
        }

        operator fun set(index: Int, value: String) {
            this@ChildClass2.set(index, value) // Noncompliant {{Replace function call with indexed accessor.}}
        }
    }
}

class GenericAccessorClass2 {
    operator fun <T> get(key: String): T = TODO()
}

