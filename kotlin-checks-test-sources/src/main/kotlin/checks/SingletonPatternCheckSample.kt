package checks

import kotlin.properties.Delegates
import kotlin.properties.ReadWriteProperty

class SingletonPatternCheckSample {

    class SimpleSingleton private constructor() {

        fun instanceMethod() {
            TODO()
        }

        companion object {
            val instance = SimpleSingleton() // Noncompliant {{Singleton pattern should use object declarations or expressions}}
//                         ^^^^^^^^^^^^^^^^^
            val x = foo() // Compliant, not a constructor call
        }
    }

    class LazyInitializationSingleton private constructor() {

        companion object {
            val instance by lazy {
                LazyInitializationSingleton() // Noncompliant {{Singleton pattern should use object declarations or expressions}}
//              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
            }
        }
    }

    class DelegationButNotLazy private constructor() {

        companion object {
            val instance: DelegationButNotLazy by guarded<DelegationButNotLazy> {
                DelegationButNotLazy() // Compliant, delegation is no lazy initialisation
            }
        }
    }

    class NotASingleton private constructor() {

        companion object {
            val instance1 = NotASingleton() // Compliant, not a singleton
            val instance2 by lazy {
                NotASingleton() // Compliant, not a singleton
            }
        }
    }

    class PublicConstructor constructor() {

        companion object {
            val defaultInstance = PublicConstructor() // Compliant, class has non-private constructors
        }
    }

    class ReuseConstructorForDelegation private constructor(
        private val id: Int
    ) {
        private constructor(): this(42)

        companion object {
            val defaultInstance = ReuseConstructorForDelegation(23) // Noncompliant {{Singleton pattern should use object declarations or expressions}}
        }
    }

    class UseNonDefaultConstructor private constructor(
        private val id: Int
    ) {
        private constructor(): this(42)

        companion object {
            val defaultInstance = UseNonDefaultConstructor() // Noncompliant {{Singleton pattern should use object declarations or expressions}}
        }
    }

    class PublicNonDefaultConstructor private constructor(
        private val id: Int
    ) {
        constructor(): this(42)

        companion object {
            val defaultInstance = PublicNonDefaultConstructor(23) // Compliant, class has non-private constructors
        }
    }

    private class PublicConstructorInPrivateClass constructor()

    companion object {
        private val publicConstructorInPrivateClassInstance = PublicConstructorInPrivateClass() // Noncompliant {{Singleton pattern should use object declarations or expressions}}
    }

    private class InstanceNotInCompanionObject constructor()

    private val instanceNotInCompanionObject = InstanceNotInCompanionObject() // Compliant, property does not reside in a companion object

    class FakeInstanceField private constructor() {

        companion object {
            val instance by lazy {
                FakeInstanceField() // Compliant, because not the initialization value
                42
            }
        }
    }

    class NotFakeInstanceField private constructor(
        val id: Int
    ) {

        companion object {
            val instance by lazy {
                val id = 42
                NotFakeInstanceField(id) // Noncompliant {{Singleton pattern should use object declarations or expressions}}
            }
        }
    }

}

fun foo(): String = TODO()

fun <T> guarded(initializer: () -> T): ReadWriteProperty<Any?, T> = TODO()