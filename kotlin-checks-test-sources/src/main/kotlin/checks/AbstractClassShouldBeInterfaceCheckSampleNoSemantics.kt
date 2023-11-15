package checks

class AbstractClassShouldBeInterfaceCheckSampleNoSemantics {

    abstract class A { // Noncompliant
        abstract fun foo()
    }

    interface Foo { // Compliant
    }
    abstract class B : Foo { // Compliant
    }

    open class C {}

    abstract class D : C() { // Compliant
    }
}