package checks

class InterfaceCouldBeFunctionalCheckSample {

    interface FunFoo : BarParent { // Compliant
        fun doSomething(args: String): String
    }

    interface IntMapper<T> { // Noncompliant {{Make this interface functional or replace it with a function type.}}
//  ^^^^^^^^^
        fun map(value: Int): T
    }

    fun interface FunctionalIntMapper<T> { // Compliant
        fun map(value: Int): T
    }

    @FunctionalInterface // Noncompliant {{"@FunctionalInterface" annotation has no effect in Kotlin}}
//  ^^^^^^^^^^^^^^^^^^^^
    interface IntMapperWithWrongAnnotation<T> { // Noncompliant {{Make this interface functional or replace it with a function type.}}
        fun map(value: Int): T
    }

    @FunctionalInterface // Noncompliant {{"@FunctionalInterface" annotation has no effect in Kotlin}}
    fun interface FunctionalIntMapperWithWrongAnnotation<T> { // Compliant
        fun map(value: Int): T
    }

    @AnyOtherAnnotation // Compliant
    interface WithAnyOtherAnnotation<T> { // Noncompliant {{Make this interface functional or replace it with a function type.}}
        fun map(value: Int): T
    }

    @AnyOtherAnnotation // Compliant
    fun interface FunctionalInterfaceAnyOtherAnnotation<T> { // Compliant
        fun map(value: Int): T
    }

    interface EmptyInterface { // Compliant
    }

    annotation class AnyOtherAnnotation

    interface TwoFunctionInterface<T> { // Compliant
        fun map(value: Int): T
        fun nap(value: Int): T
    }

    interface FunctionAndPropertyInterface<T> { // Compliant
        fun map(value: Int): T
        val limit: Int
    }

    abstract class AbstractClass<T> { // Compliant
        abstract fun map(value: Int): T
    }

    class NonAbstractClass { // Compliant
        fun map(value: Int) = value * value
    }

    interface NonFunctionalOuterInterface { // Noncompliant {{Make this interface functional or replace it with a function type.}}

        fun map(value: Int): Int

        interface NonFunctionalInnerInterface { // Noncompliant {{Make this interface functional or replace it with a function type.}}
            fun map(value: Int): Int
        }

        fun interface FunctionalInnerInterface { // Compliant
            fun map(value: Int): Int
        }

        companion object {
            val size = 42
        }
    }

    fun interface FunctionalOuterInterface { // Compliant

        fun map(value: Int): Int

        interface NonFunctionalInnerInterface { // Noncompliant {{Make this interface functional or replace it with a function type.}}
            fun map(value: Int): Int
        }

        fun interface FunctionalInnerInterface { // Compliant
            fun map(value: Int): Int
        }

        companion object {
            val size = 42
        }
    }

    interface NonFunctionalOuterTwoFunctionInterface { // Compliant

        fun map(value: Int): Int
        fun nap(value: Int): Int

        interface NonFunctionalInnerInterface { // Noncompliant {{Make this interface functional or replace it with a function type.}}
            fun map(value: Int): Int
        }

        fun interface FunctionalInnerInterface { // Compliant
            fun map(value: Int): Int
        }

        companion object {
            val size = 42
        }
    }

    interface NonFunctionalOuterFunctionAndPropertyInterface { // Compliant

        fun map(value: Int): Int
        val limit: Int

        interface NonFunctionalInnerInterface { // Noncompliant {{Make this interface functional or replace it with a function type.}}
            fun map(value: Int): Int
        }

        fun interface FunctionalInnerInterface { // Compliant
            fun map(value: Int): Int
        }

        companion object {
            val size = 42
        }
    }


    interface BarParent {
        fun parentFun1()
        fun parentFun2()
    }

    sealed interface SealedInterface { // Compliant: functional interfaces cannot be sealed
        fun map(value: Int): Int
    }

}

// We shouldn't suggest the fun interface in the case of a single generic function

interface DistributedLock { // Compliant
    fun <R> transaction(body: () -> R): R
}

class RedisDistributedLock : DistributedLock {
    override fun <R> transaction(body: () -> R): R {
        return null as R
    }
}

// We shouldn't suggest the fun interface in the case of a single function containing default parameters
interface Foo {
    fun bar(baz: Int = 0)
}
