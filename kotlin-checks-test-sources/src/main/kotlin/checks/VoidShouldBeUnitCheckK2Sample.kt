package checks

typealias RecursiveTypeAlias1 = (value: Enum<*>?) -> Unit? // Compliant

private typealias TypeAlias1 = () -> Map<*, Void>  // Noncompliant {{Replace this usage of `Void` type with `Unit`.}}
//                                          ^^^^

typealias Action1 = () -> Void // Noncompliant {{Replace this usage of `Void` type with `Unit`.}}
//                        ^^^^

typealias LLAction1 = List<Function1<List<String>, Function2<Set<Int>, List<List<List<Void>>>, otherpackage.Void>>> // Noncompliant {{Replace this usage of `Void` type with `Unit`.}}
//                                                                                    ^^^^

fun myFun1(a: Action1) {
    println(a)
}

interface TestA1<out T> {
    fun foo(): T
}

abstract class TestB1 : TestA1<Void?> { // Compliant, for java interop due to https://youtrack.jetbrains.com/issue/KT-15964
    override fun foo(): Void? { return null } // Compliant, overridden function
}
abstract class TestC1 : TestA1<Void> { // Compliant, for java interop due to https://youtrack.jetbrains.com/issue/KT-15964
    abstract override fun foo(): Void // Compliant, overridden function
}
open class TestD1 : TestA1<Void> { // Compliant, for java interop due to https://youtrack.jetbrains.com/issue/KT-15964
    override fun foo(): Void { TODO() } // Compliant, overridden function
}

abstract class TestE1 : TestA1<Void> { // Compliant, for java interop due to https://youtrack.jetbrains.com/issue/KT-15964

}

class TestF1 : TestA1<Void?> { // Noncompliant
    override fun foo(): Void? { return null } // Compliant, overridden function
}

fun s1() : TestA1<Void?> = TODO() // Noncompliant {{Replace this usage of `Void` type with `Unit`.}}


typealias ActionUnit1 = () -> Unit


enum class C1 {
    A, B
}

class VoidShouldBeUnitCheckSample1 {

    fun f(c: C1): Void? { // Noncompliant {{Replace this usage of `Void` type with `Unit`.}}
//                ^^^^^
        when (val x = c) {
            C1.A -> println()
            C1.B -> println()
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


interface WithVoidFunctions1 {
    fun voidFunction1(): Void // Noncompliant
    fun voidFunction2(): Void // Noncompliant
}
