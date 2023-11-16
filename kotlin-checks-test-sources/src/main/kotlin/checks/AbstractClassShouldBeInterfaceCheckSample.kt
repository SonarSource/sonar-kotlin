package checks

class AbstractClassShouldBeInterfaceCheckSample {

    abstract class ShapeA { // Noncompliant {{Replace this abstract class with an interface, or add function implementations or state properties to the class.}}
//                 ^^^^^^
        abstract fun getPath(): Int
        abstract fun getBoundingBox(): Pair<Int,Int>
    }

    interface ShapeB { // Compliant, we are using an interface here
        fun getPath(): Int
        fun getBoundingBox(): Pair<Int,Int>
    }


    abstract class ShapeC { // Compliant, abstract class has function implementations
        abstract fun getPath(): Int
        fun getBoundingBox(): Pair<Int,Int> {
            return Pair(0,0)
        }
    }

    abstract class Foo { // Noncompliant
        abstract var bar: String
        abstract val baz: String
    }

    abstract class Bar { // compliant
        var bar: String = ""
        abstract val baz: String
    }

    abstract class A { // Noncompliant
        abstract class Inner { // Noncompliant
            abstract fun foo()
        }
    }

    abstract class B { // Compliant
        abstract class Inner {
            fun foo() {}
        }
    }

    abstract class C { // Noncompliant
        object Inner {

        }
    }


    abstract class D { // Compliant
        companion object Inner {
            fun foo() {}
        }
    }

    interface E { // Compliant
        object Inner {

        }
    }

    abstract class F { // Compliant, at least one property in the initializer
        init {
            val x = 0
        }
    }

    abstract class G { // Compliant
        constructor() {
            val x = 0
        }
    }

    abstract class I { // Noncompliant
        init {
        }
    }

    abstract class J { // Compliant
        constructor() {
        }
    }

    abstract class K {} // Noncompliant
    abstract class L : K() {} // compliant, extend an abstract as he may have no other choice

    interface M {} // compliant
    abstract class N : M {} // Noncompliant
    abstract class O : K(), M {} // compliant

    abstract interface ZZZ { // Compliant
    }
}
