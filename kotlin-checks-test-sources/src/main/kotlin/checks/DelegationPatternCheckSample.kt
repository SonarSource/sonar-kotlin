package checks

interface Network {
    fun connect(url: String)
}

class PoorNetwork : Network {
    override fun connect(url: String) {
        TODO()
    }
}

class GoodNetwork : Network {
    override fun connect(url: String) {
        TODO()
    }
}

interface Graphics {
    fun render(): Int
}

class Nvidia : Graphics {
    override fun render(): Int {
        TODO()
    }
}

class AsusCardFrom2010 : Graphics {
    override fun render(): Int {
        TODO()
    }
}

abstract class OS : Network, Graphics

class LinuxSample: OS() {
    private val network = GoodNetwork()
    override fun connect(url: String) = network.connect(url) // Noncompliant {{Replace with interface delegation using "by" in the class header.}}
//               ^^^^^^^
    private val graphics= Nvidia()
    override fun render() = graphics.render() // Noncompliant {{Replace with interface delegation using "by" in the class header.}}
}

class WindowsSample: OS() {
    private val network = PoorNetwork()
    override fun connect(url: String) = network.connect(url) // Noncompliant {{Replace with interface delegation using "by" in the class header.}}
    private val graphics = AsusCardFrom2010()
    override fun render() = graphics.render() // Noncompliant {{Replace with interface delegation using "by" in the class header.}}
}

class FreeRtosSample: OS() {
    private val network = PoorNetwork()
    override fun connect(url: String) = network.connect("file:///"+url) // Compliant, arguments not passed 1 to 1
    private val graphics = AsusCardFrom2010()
    override fun render() = graphics.render() + 42 // Compliant, return type not passed 1 to 1
}

class SubInterfaceDelegationSample: SimpleInterface {
    val subInterface: SubInterface = TODO()

    override fun function1() { // Noncompliant {{Replace with interface delegation using "by" in the class header.}}
        subInterface.function1()
    }

    fun function2() { // Compliant, function2 is not part of SimpleInterface
        subInterface.function2()
    }
}

interface SimpleInterface {
    fun function1()
}

interface SubInterface: SimpleInterface {
    fun function2()
}

class ParameterizedInterfaceSample<P>: ParameterizedInterface<P>, ParameterizedSubInterface<P>, AnyParent() {
    val parameterizedSubSubInterface: ParameterizedSubSubInterface<P> = TODO()

    fun buf(p: P) { // Compliant
        parameterizedSubSubInterface.function1(p)
    }

    override fun function1(p: P) { // Noncompliant {{Replace with interface delegation using "by" in the class header.}}
        parameterizedSubSubInterface.function1(p)
    }

    override fun function2(p: P):P = parameterizedSubSubInterface.function2(p) // Noncompliant {{Replace with interface delegation using "by" in the class header.}}

    override fun function3(p: P):P { // Noncompliant {{Replace with interface delegation using "by" in the class header.}}
        return parameterizedSubSubInterface.function3(p)
    }

    companion object
}

open class AnyParent: AnyGrandParent()

open class AnyGrandParent

interface ParameterizedInterface<P> {
    fun function1(p: P)

    fun function2(p: P): P
}

interface ParameterizedSubInterface<Q>: ParameterizedInterface<Q> {
    fun function3(p: Q): Q
}

interface ParameterizedSubSubInterface<P>: ParameterizedSubInterface<P> {
    fun function4(p: P): P
}

class ComplexFunctionSignatureSample<U: List<Int?>>: ComplexFunctionSignature<Int?, U> {

    val delegee: ComplexFunctionSignature<Int?, U> = TODO()
    override fun <P, Q : Map<P, U>> function1(arg1: Q, arg2: P?, arg3: List<Int?>, optionalArg: Int): List<U> { // Noncompliant {{Replace with interface delegation using "by" in the class header.}}
//                                  ^^^^^^^^^
        return delegee.function1(arg1, arg2, arg3, optionalArg)
    }
}

interface ComplexFunctionSignature<T, U: List<T>> {
    fun <P, Q: Map<P, U>>function1(arg1: Q, arg2: P?, arg3: List<T>, optionalArg: Int = 42): List<U>
}

class WithDifferentArgNamesSample: WithDifferentArgNames {

    val withDifferentArgNames: WithDifferentArgNames = TODO()

    override fun function1(first: Int, second: Boolean) { // Noncompliant {{Replace with interface delegation using "by" in the class header.}}
        withDifferentArgNames.function1(first, second)
    }
}

interface WithDifferentArgNames {
    fun function1(a: Int, b: Boolean)
}

class WithNonPublicOverride: SimpleInterface {
    override fun function1() { // Compliant, non-trivial receiver expression
        getDelegee().function1()
    }

    private fun getDelegee(): SimpleInterface = TODO()

    internal fun function2() {
        // ...
    }

    fun function3() {
        // ...
    }
}

interface OtherSimpleInterface {
    fun function1()
}

class NoCommonSuperInterfaces: SimpleInterface {
    val otherSimpleInterface: OtherSimpleInterface = TODO()
    override fun function1() { // Compliant, function does not override anything in OtherSimpleInterface
        otherSimpleInterface.function1()
    }
}

interface StaticRenderResult {
    val render: Int
}

class OverrideIsNoFunctionCall: Graphics {
    val staticResult: StaticRenderResult = TODO()
    override fun render(): Int = staticResult.render // Compliant, not a function call
}

interface BetterGraphics: Graphics {
    fun render(timestamp: Long): Number
}

class DifferentReturnType: BetterGraphics {
    val graphics: BetterGraphics = TODO()
    override fun render(): Int = TODO()
    override fun render(timestamp: Long): Number = graphics.render() // Compliant, return types do not match
}

interface Interface1

interface ChildInterface1: Interface1 {

    fun function1(a: Int, b: Int)

    fun function2(a: Int)
}

interface ChildInterface2: Interface1 {
    fun function2(a: Number)
}

class WithSwappedParameters: ChildInterface1 {

    val child1: ChildInterface1 = TODO()

    val child2: ChildInterface2 = TODO()

    override fun function1(a: Int, b: Int) = child1.function1(b, a) // Compliant, arguments are not in the same order

    override fun function2(a: Int) = child2.function2(a) // Compliant, arguments do not have the same type
}

class WithSwappedNameAndParameters: ChildInterface1 {

    val child1: ChildInterface1 = TODO()

    override fun function1(b: Int, a: Int) = child1.function1(a, b) // Compliant, arguments are not in the same order
    override fun function2(a: Int) = TODO()
}
