package checks

fun equals(other: Any?): Boolean { // Compliant
    val b = Any()
    if (other?.javaClass == b?.javaClass) return true

    return false
}

abstract class EqualsArgumentTypeCheckSample {

    open class MyClass2 {
        val a = 1
        override fun equals(other: Any?): Boolean { // Compliant
            if (other !is MyClass3)
                return false
            val mc = other
            mc.a
            // ...
            return this === mc
        }
    }

    interface Test {
        override fun equals(other: Any?): Boolean // Compliant
    }

    open class Foo4 {
        val a = 1
        override fun equals(other: Any?): Boolean { // Noncompliant
            val b = Any()
            if (other?.javaClass == b?.javaClass) return true

            return false
        }

        fun equalsTesting(other: Any?): Boolean { // Compliant
            val b = Any()
            if (other?.javaClass == b.javaClass) return true

            return false
        }
    }

    open class Foo6 {
        val a = 1
        override fun equals(other: Any?): Boolean { // Noncompliant
            val b = Any()
            if (other?.javaClass == b.javaClass) return true

            return false
        }
    }

    open class NewClass {
        override fun equals(other: Any?): Boolean { // Compliant
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NewClass
            // ...
            return true
        }
    }

    class NewClass2 : NewClass() {
        override fun equals(other: Any?): Boolean { // Compliant
            if (other !is NewClass) return false

            // ...
            return true
        }
    }

    class FooNoncompliant {
        val a = 1
        override fun equals(other: Any?): Boolean { // Noncompliant
            return (other as? String)?.let { true } ?: false
        }
    }


    class Foo0 {
        val a = 1
        override fun equals(other: Any?): Boolean { // Compliant
            if ((other as? Foo0)?.a == 2) return true

            return false
        }
    }

    class Foo1 {
        override fun equals(other: Any?): Boolean { // Compliant
            return (other as? Foo1)?.let { true } ?: false
        }
    }

    class Foo2 {
        override fun equals(other: Any?): Boolean { // Compliant
            return (other is Foo2).let {
                // ...
                true
            }
        }
    }

    override fun equals(other: Any?): Boolean { // Noncompliant
//               ^^^^^^
        return 1 is Int
    }

    class Foo {
        override fun equals(other: Any?): Boolean { // Noncompliant
            return 1 is Int
        }
    }


    sealed class NewClass3 {
        class Single : NewClass3() {
            override fun equals(other: Any?): Boolean { // Compliant
                if (other !is NewClass3.Single) return false
                //...
                return true
            }
        }
    }

    class NewClass4 {
        override fun equals(other: Any?): Boolean { // Noncompliant
//                   ^^^^^^
            val anyObject = null
            if (anyObject is NewClass4) return false
            //...
            return true
        }
    }

    class NewClass5 {
        override fun equals(other: Any?): Boolean { // Noncompliant
            if (other is NewClass4) return false
            //...
            return true
        }
    }

    class MyClass {
        override fun equals(other: Any?): Boolean { // Noncompliant {{Add a type test to this method.}}
            val mc = other as MyClass
            // ...
            return this == mc
        }
    }

    class MyClass1 {
        val a = 1
        override fun equals(other: Any?): Boolean { // Compliant
            if (other is MyClass1) {
                val mc = other
                mc.a
                // ...
                return this === mc
            }
            return false
        }
    }


    class MyClass3 : MyClass2() {
        class Klas1 {
            val a = 1
        }

        override fun equals(other: Any?): Boolean { // Noncompliant
            if (other !is Klas1)
                return false
            val mc = other
            mc.a
            // ...
            return this === mc
        }
    }

    class MyClass4 {
        override fun equals(other: Any?): Boolean { // Compliant
            if (other?.javaClass != MyClass4::class.java)
                return false
            val mc = other as MyClass4
            // ...
            return this == mc
        }
    }

    class MyClass5 {
        override fun equals(other: Any?): Boolean { // Compliant
            if (other?.javaClass != this.javaClass)
                return false
            val mc = other as MyClass5
            // ...
            return this == mc
        }
    }

    class MyClass6 {
        override fun equals(other: Any?): Boolean { // Compliant
            if (this.javaClass != other?.javaClass)
                return false
            val mc = other as MyClass6
            // ...
            return this == mc
        }
    }

    class MyClass7 {
        override fun equals(other: Any?): Boolean { // Compliant
            if (other?.javaClass == this.javaClass) {
                val mc = other as MyClass7
                // ...
                return this == mc
            }
            return false
        }
    }

    class MyClass8 {
        override fun equals(other: Any?): Boolean { // Noncompliant
            if (other != null) {
                val mc = other as MyClass8
                // ...
                return this == mc
            }
            return false
        }
    }

}
