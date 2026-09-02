package checks

import android.util.LongSparseArray
import android.util.SparseArray
import android.util.SparseBooleanArray
import android.util.SparseIntArray
import android.util.SparseLongArray
import com.google.common.collect.ImmutableList
import com.google.common.collect.RangeMap
import com.google.common.collect.Table
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.util.BitSet
import java.util.Calendar
import java.util.Stack
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.concurrent.atomic.AtomicLongArray
import java.util.concurrent.atomic.AtomicReferenceArray
import okhttp3.Headers
import otherpackage.KotlinLibContainer
import otherpackage.get

class IndexedAccessCheckSample {

    fun withoutIndexedAccessors(
        list: MutableList<Int>,
        map: MutableMap<String, Int>,
        grid: Grid,
        value: Any,
        buffer: ByteBuffer,
        stack: Stack<String>,
        atomicArray: AtomicIntegerArray,
        atomicLongArray: AtomicLongArray,
        atomicReferenceArray: AtomicReferenceArray<String>,
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
        atomicLongArray.get(0) // Noncompliant {{Replace function call with indexed accessor.}}
        atomicLongArray.set(0, 42L) // Noncompliant {{Replace function call with indexed accessor.}}
        atomicReferenceArray.get(0) // Noncompliant {{Replace function call with indexed accessor.}}
        atomicReferenceArray.set(0, "value") // Noncompliant {{Replace function call with indexed accessor.}}
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

    fun bufferBulkReads(byteBuffer: ByteBuffer, charBuffer: CharBuffer, bytes: ByteArray, chars: CharArray) {
        byteBuffer.get(bytes) // Compliant - copies into the destination instead of selecting an element
        byteBuffer.get(bytes, 0, bytes.size) // Compliant - relative bulk read
        byteBuffer.get(0, bytes) // Compliant - absolute bulk read
        byteBuffer.get(0, bytes, 0, bytes.size) // Compliant - absolute bulk read with destination range
        charBuffer.get(chars) // Compliant - all Buffer specializations follow the same signature rules
        charBuffer.get(0) // Noncompliant {{Replace function call with indexed accessor.}}
    }

    fun bitSetRanges(bitSet: BitSet) {
        bitSet.get(1, 4) // Compliant - returns a range, not the element at two indexes
        bitSet.set(1) // Compliant - cannot be expressed as indexed assignment
        bitSet.set(1, 4) // Compliant - sets a range to true
        bitSet.set(1, 4, false) // Compliant - sets a range to a value
    }

    fun androidSparseArrays(
        sparseArray: SparseArray<String>,
        sparseIntArray: SparseIntArray,
        sparseBooleanArray: SparseBooleanArray,
        sparseLongArray: SparseLongArray,
        longSparseArray: LongSparseArray<String>,
    ) {
        sparseArray.get(1) // Noncompliant {{Replace function call with indexed accessor.}}
        sparseArray.get(1, "default") // Compliant - the second argument is a default value
        sparseArray.set(1, "value") // Noncompliant {{Replace function call with indexed accessor.}}
        sparseIntArray.get(1) // Noncompliant {{Replace function call with indexed accessor.}}
        sparseIntArray.get(1, 42) // Compliant - the second argument is a default value
        sparseIntArray.set(1, 42) // Noncompliant {{Replace function call with indexed accessor.}}
        sparseBooleanArray.get(1) // Noncompliant {{Replace function call with indexed accessor.}}
        sparseBooleanArray.get(1, false) // Compliant - the second argument is a default value
        sparseBooleanArray.set(1, true) // Noncompliant {{Replace function call with indexed accessor.}}
        sparseLongArray.get(1) // Noncompliant {{Replace function call with indexed accessor.}}
        sparseLongArray.get(1, 42L) // Compliant - the second argument is a default value
        sparseLongArray.set(1, 42L) // Noncompliant {{Replace function call with indexed accessor.}}
        longSparseArray.get(1L) // Noncompliant {{Replace function call with indexed accessor.}}
        longSparseArray.get(1L, "default") // Compliant - the second argument is a default value
        longSparseArray.set(1L, "value") // Noncompliant {{Replace function call with indexed accessor.}}
    }

    fun kotlinLibraryTypes(container: KotlinLibContainer<String>, headers: Headers) {
        // Compiled Kotlin library types with operator fun get/set should still be flagged.
        // These are NOT Java interop operators — they are genuine Kotlin operators from a library.
        container.get(0) // Noncompliant {{Replace function call with indexed accessor.}}
        container.set(0, "value") // Noncompliant {{Replace function call with indexed accessor.}}
        headers.get("Content-Type") // Noncompliant {{Replace function call with indexed accessor.}}
    }

    fun javaLibraryTypes(
        immutableList: ImmutableList<String>,
        table: Table<String, String, Int>,
        rangeMap: RangeMap<Int, String>,
    ) {
        // Java library types extending List/Map — allowed Java interop, should still raise
        immutableList.get(0) // Noncompliant {{Replace function call with indexed accessor.}}
        // Java library types NOT extending List/Map — Java interop excluded
        table.get("row", "col") // Compliant - Java interop operator, not in allowed types
        rangeMap.get(42) // Compliant - Java interop operator, not in allowed types
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

class ChainableBuilder {
    operator fun set(key: String, value: String): ChainableBuilder {
        return this
    }

    operator fun get(key: String): String = ""

    fun build(): String = ""
}

class ChainableMap {
    operator fun set(key: String, value: Int): ChainableMap {
        return this
    }

    operator fun get(key: String): Int = 0

    fun size(): Int = 0
}

fun setCallUsages(builder: ChainableBuilder, chainableMap: ChainableMap, list: MutableList<Int>) {
    builder.set("standalone", "value") // Noncompliant {{Replace function call with indexed accessor.}}
    builder.set("a", "1").set("b", "2").set("c", "3") // Compliant - every set belongs to the same fluent chain
    builder.set("a", "1").set("b", "2") // Compliant - terminal set is preceded by another set
    (builder.set("a", "1")).set("b", "2") // Compliant - parentheses do not break the setter chain
    (builder.`set`("a", "1") as ChainableBuilder).set("b", "2") // Compliant - casts and backticks do not break the setter chain
    builder.set("a", "1")!!.set("b", "2") // Compliant - non-null assertions do not break the setter chain
    builder.set("a", "1").build() // Compliant - set result used in chain
    val setResult = builder.set("a", "1") // Compliant - set result is assigned
    consumeBuilder(builder.set("a", "1")) // Compliant - set result is passed as an argument
    val previousValue = list.set(0, 42) // Compliant - indexed assignment would discard the returned old value
    consumeInt(previousValue)

    builder.get("a").length // Noncompliant {{Replace function call with indexed accessor.}}

    chainableMap.set("a", 1).set("b", 2) // Compliant - terminal set is preceded by another set
    chainableMap.set("a", 1).size() // Compliant - set result used in chain

    createBuilder().set("a", "1") // Noncompliant {{Replace function call with indexed accessor.}}
}

fun consumeBuilder(builder: ChainableBuilder) = Unit

fun createBuilder() = ChainableBuilder()

fun consumeInt(value: Int) = Unit
