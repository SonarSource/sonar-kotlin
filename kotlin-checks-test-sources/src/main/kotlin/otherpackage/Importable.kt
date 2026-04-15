package otherpackage

import java.io.InputStream

operator fun Any.get(index: Any) = this
operator fun String.plus(other: String) = this

object OtherClass {
    operator fun Int.get(value: Int) = this
    operator fun Int.set(value1: Int, value2: Int) {}
    operator fun plus(other: OtherClass) = this
    operator fun InputStream.minus(other: InputStream) = this
}

object OtherClass2 {
    operator fun InputStream.plus(other: InputStream) = this
    operator fun InputStream.get(value: Int) = this
    operator fun InputStream.set(value1: Int, value2: Int) {}
}

class ClassUsedViaConstructorReference1
class ClassUsedViaConstructorReference2
class ClassUsedViaConstructorReference3

fun functionTakingAny(value: Any) {}

fun String.stringExtFun1() {}
fun String.stringExtFun2() {}
infix fun String.someInfixFun(foo: String) = this

/**
 * A Kotlin class with operator fun get/set, simulating a third-party Kotlin library type
 * (e.g., Arrow, KotlinX collections, or any user-defined library).
 * When resolved from compiled class files (not source), symbol.psi will be null.
 */
class KotlinLibContainer<T>(private val items: List<T>) {
    operator fun get(index: Int): T = items[index]
    operator fun set(index: Int, value: T) { /* no-op for test */ }
}
