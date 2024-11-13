package checks

typealias Action = () -> Void // FN

fun myFun(a: Action) {
    println(a)
}

interface TestA<out T> {
    fun foo(): T
}

abstract class TestB : TestA<Void?> { // Compliant, for java interop due to https://youtrack.jetbrains.com/issue/KT-15964
    override fun foo(): Void? { return null } // Compliant, overridden function
}
abstract class TestC : TestA<Void> { // Compliant, for java interop due to https://youtrack.jetbrains.com/issue/KT-15964
    abstract override fun foo(): Void // Compliant, overridden function
}
open class TestD : TestA<Void> { // Compliant, for java interop due to https://youtrack.jetbrains.com/issue/KT-15964
    override fun foo(): Void { TODO() } // Compliant, overridden function
}

abstract class TestE : TestA<Void> { // Compliant, for java interop due to https://youtrack.jetbrains.com/issue/KT-15964

}

class TestF : TestA<Void?> { // Noncompliant
    override fun foo(): Void? { return null } // Compliant, overridden function
}

fun s() : TestA<Void?> = TODO() // Noncompliant {{Replace this usage of `Void` type with `Unit`.}}


typealias ActionUnit = () -> Unit


enum class C {
    A, B
}

class VoidShouldBeUnitCheckSample {

    fun f(c: C): Void? { // Noncompliant {{Replace this usage of `Void` type with `Unit`.}}
//               ^^^^^
        when (val x = c) {
            C.A -> println()
            C.B -> println()
        }
        return null
    }
    fun f1(): Void { // Noncompliant
        return Void.TYPE.newInstance() // Compliant
    }

    fun f2(): Void { // Noncompliant
        return Void.TYPE.getDeclaredConstructor().newInstance() // Compliant
    }

    var x: () -> Void? = { null } // Noncompliant

    fun f3(x: Function<Void>) {} // Noncompliant

    fun f4(x: Function<out Void>) {} // Noncompliant

    var x1: () -> Unit? = { null }

    fun f5(x: Function<Unit>) {}

    fun f6(x: Function<out Unit>) {}

    fun f7(): Unit {
        return Unit
    }
}


interface WithVoidFunctions {
    fun voidFunction1(): Void // Noncompliant
    fun voidFunction2(): Void // Noncompliant
}
