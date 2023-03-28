package checks

typealias ActionN = () -> Void

typealias LLActionN = List<Function1<List<String>, Function2<Set<Int>, List<List<List<Void>>>, otherpackage.Void>>>

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

class TestFN : TestAN<Void?> { 
    override fun foo(): Void? { return null }
}

fun sN() : TestAN<Void?> = TODO()


interface WithVoidFunctionsN {
    fun voidFunction1(): Void 
    fun voidFunction2(): Void 
}