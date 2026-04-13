package checks

import otherpackage.get
import java.nio.ByteBuffer
import java.util.BitSet
import java.util.Calendar
import java.util.Stack
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray

class IndexedAccessCheckSample {

    fun withoutIndexedAccessors(
        list: MutableList<Int>,
        map: MutableMap<String, Int>,
        grid: Grid,
        value: Any,
        buffer: ByteBuffer,
        stack: Stack<String>,
        atomicArray: AtomicIntegerArray,
        bitSet: BitSet,
        arrayList: ArrayList<Int>,
        hashMap: HashMap<String, Int>,
    ) {
        list.get(1) // Noncompliant {{Replace function call with indexed accessor.}}
//           ^^^
        list.set(1, 42) // Noncompliant {{Replace function call with indexed accessor.}}
//           ^^^
        map.get("b") // Noncompliant {{Replace function call with indexed accessor.}}
        map.set("b", 42) // Noncompliant {{Replace function call with indexed accessor.}}
//          ^^^
        grid.get(1, 2) // Noncompliant {{Replace function call with indexed accessor.}}
        grid.set(1, 2, 42) // Noncompliant {{Replace function call with indexed accessor.}}
        value.get(42) // Noncompliant {{Replace function call with indexed accessor.}}
        // Java interop allowed types - indexed access is idiomatic for these
        buffer.get(0) // Noncompliant {{Replace function call with indexed accessor.}}
        stack.get(0) // Noncompliant {{Replace function call with indexed accessor.}}
        atomicArray.get(0) // Noncompliant {{Replace function call with indexed accessor.}}
        atomicArray.set(0, 42) // Noncompliant {{Replace function call with indexed accessor.}}
        bitSet.get(5) // Noncompliant {{Replace function call with indexed accessor.}}
        bitSet.set(5, true) // Noncompliant {{Replace function call with indexed accessor.}}
        // Concrete Java collection implementations - caught via List/Map supertype check
        arrayList.get(0) // Noncompliant {{Replace function call with indexed accessor.}}
        arrayList.set(0, 42) // Noncompliant {{Replace function call with indexed accessor.}}
        hashMap.get("key") // Noncompliant {{Replace function call with indexed accessor.}}
    }

    fun javaInteropExcluded(cal: Calendar, future: CompletableFuture<Int>) {
        // Java get/set methods are operators via Java-Interop (https://kotlinlang.org/docs/java-interop.html#operators)
        // but indexed access is not idiomatic for these types, so we don't raise
        cal.get(Calendar.YEAR) // Compliant - Java interop operator, not in allowed types
        cal.set(Calendar.YEAR, 2024) // Compliant - Java interop operator, not in allowed types
        future.get(1L, TimeUnit.SECONDS) // Compliant - Java interop operator, not in allowed types
    }

    fun withIndexedAccessors(lisp: Lisp<Int>, maybeNullList: MutableList<Int>?,  list: MutableList<Int>, map: MutableMap<String, Int>, grid: Grid, num: AtomicInteger, root: GenericAccessorClass) {
        num.get() // Compliant, class doesn't have an index access operator
        lisp.get(index = 1) // Compliant, named arguments are allowed
        grid.get(row = 1, 2) // Compliant, named arguments are allowed
        lisp.get(1) // Compliant, not an operator
        list[1] // Compliant
        list[1] = 42 // Compliant
        map["b"] // Compliant
        map["b"] = 42 // Compliant
        grid[1, 2] // Compliant
        grid[1, 2] = 42 // Compliant
        list.getOrNull(2) // Complaint, not an operator
        list.getOrElse(3) {42} // Complaint, not an operator
        map.getValue("a") // Complaint, not an operator
        map.getOrElse("c") {42} // Complaint, not an operator
        root.get<String>("id") // Compliant: explicit type parameter cannot be expressed with [] syntax
        maybeNullList?.get(0) // Compliant: safe call uses KtSafeQualifiedExpression, not KtDotQualifiedExpression
    }
}

interface Grid {
    operator fun get(row: Int, column: Int): Int
    operator fun set(row: Int, column: Int, value: Int)
}

interface Lisp<T> {
    fun get(index: Int)
}

open class ParentClass {
    private val _array = mutableListOf<String>()

    open operator fun get(index: Int): String {
        return _array[index]
    }

    open operator fun set(index: Int, value: String) {
        _array[index] = value
    }

    val size: Int get() = _array.size
}

class ChildClass : ParentClass() {

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
            return this@ChildClass.get(index) // Noncompliant {{Replace function call with indexed accessor.}}
        }

        operator fun set(index: Int, value: String) {
            this@ChildClass.set(index, value) // Noncompliant {{Replace function call with indexed accessor.}}
        }
    }
}

class GenericAccessorClass {
    operator fun <T> get(key: String): T = TODO()
}

