package checks

typealias ActionN = () -> Void // Noncompliant
//                        ^^^^

typealias LLActionN = List<Function1<List<String>, Function2<Set<Int>, List<List<List<Void>>>, otherpackage.Void>>> // Noncompliant
//                                                                                    ^^^^

fun myFunN(a: Action) {
    println(a)
}

interface TestAN<out T> {
    fun foo(): T
}

abstract class TestBN : TestAN<Void?> {
    override fun foo(): Void? { return null }
}
abstract class TestCN : TestAN<Void> {
    abstract override fun foo(): Void
}
open class TestDN : TestAN<Void> {
    override fun foo(): Void { TODO() }
}

abstract class TestEN : TestAN<Void> {

}

class TestFN : TestAN<Void?> { // Noncompliant
//                    ^^^^^
    override fun foo(): Void? { return null }
}

fun sN() : TestAN<Void?> = TODO() // Noncompliant
//                ^^^^^


interface WithVoidFunctionsN {
    fun voidFunction1(): Void // Noncompliant
//                       ^^^^
    fun voidFunction2(): Void // Noncompliant
//                       ^^^^
}


interface MonoN<T>

interface WebFilterN {
    fun filter(): MonoN<Void>
}

class MyFilterN : WebFilterN {
    override fun filter(): MonoN<Void> { TODO() } // Compliant, overriding function with Void as type argument
}

class MyFilterNWithLocalVar : WebFilterN {
    override fun filter(): MonoN<Void> { // Compliant, overriding function with Void as type argument
        val local: Function<Void> = TODO() // Noncompliant
//                          ^^^^
        return TODO()
    }
}

interface GenericProcessorN<T> {
    fun process(): Map<String, T>
}

class VoidProcessorN : GenericProcessorN<Void> { // Noncompliant
//                                       ^^^^
    override fun process(): Map<String, Void> { TODO() } // Compliant, overriding function with Void as type argument
}

fun nonOverrideFunWithVoidTypeArgN(): List<Void> { TODO() } // Noncompliant
//                                         ^^^^

fun nonOverrideFunWithVoidParamN(x: Function<Void>) {} // Noncompliant
//                                           ^^^^
