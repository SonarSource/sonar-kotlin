package checks

typealias RecursiveTypeAlias = (value: Enum<*>?) -> Unit? // Compliant

private typealias TypeAlias = () -> Map<*, Void>  // Noncompliant {{Replace this usage of `Void` type with `Unit`.}}
//                                         ^^^^

typealias Action = () -> Void // Noncompliant {{Replace this usage of `Void` type with `Unit`.}}
//                       ^^^^

typealias LLAction = List<Function1<List<String>, Function2<Set<Int>, List<List<List<Void>>>, otherpackage.Void>>> // Noncompliant {{Replace this usage of `Void` type with `Unit`.}}
//                                                                                   ^^^^

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


interface Mono<T>

interface WebFilter {
    fun filter(): Mono<Void>
}

class MyFilter : WebFilter {
    override fun filter(): Mono<Void> { TODO() } // Compliant, overriding function with Void as type argument
}

class MyFilterWithLocalVar : WebFilter {
    override fun filter(): Mono<Void> { // Compliant, overriding function with Void as type argument
        val local: Function<Void> = TODO() // Noncompliant
        return TODO()
    }
}

interface GenericProcessor<T> {
    fun process(): Map<String, T>
}

class VoidProcessor : GenericProcessor<Void> { // Noncompliant
    override fun process(): Map<String, Void> { TODO() } // Compliant, overriding function with Void as type argument
}

interface MultiGenericService<T> {
    fun execute(): Map<String, List<T>>
}

class MyService : MultiGenericService<Void> { // Noncompliant
    override fun execute(): Map<String, List<Void>> { TODO() } // Compliant, overriding function with Void in nested type argument
}

interface ParamOverrideService {
    fun process(callback: Function<Void>)
}

class ParamOverrideImpl : ParamOverrideService {
    override fun process(callback: Function<Void>) {} // Compliant, overriding function with Void in parameter type argument
}

fun nonOverrideFunWithVoidTypeArg(): List<Void> { TODO() } // Noncompliant

fun nonOverrideFunWithVoidParam(x: Function<Void>) {} // Noncompliant
