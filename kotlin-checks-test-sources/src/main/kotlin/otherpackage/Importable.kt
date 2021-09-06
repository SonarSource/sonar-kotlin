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

fun String.stringExtFun1() {}
fun String.stringExtFun2() {}
infix fun String.someInfixFun(foo: String) = this
