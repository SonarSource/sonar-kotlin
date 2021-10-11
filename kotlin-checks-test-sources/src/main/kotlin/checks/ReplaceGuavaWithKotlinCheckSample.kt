package checks

import com.google.common.base.Function
import com.google.common.base.Optional
import com.google.common.base.Predicate
import com.google.common.base.Supplier
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet

class ReplaceGuavaWithKotlinCheckSample() {

    open class A(val p1: Predicate<String>) { // Noncompliant {{Use "(T) -> Boolean" instead.}}
        open fun foo(p: Predicate<String>) // Noncompliant {{Use "(T) -> Boolean" instead.}}
            : Predicate<String> // Noncompliant {{Use "(T) -> Boolean" instead.}}
            = p
    }

    class B(p1: Predicate<String>) : A(p1) { // Compliant, need a Predicate for A constructor
        override fun foo(p: Predicate<String>): Predicate<String> // Compliant, forced to override a specific type
            = p
    }

    class C(p: (String?) -> Boolean) : A(Predicate<String>(p)) { // Compliant
        fun bar(p: (String?) -> Boolean) { // Compliant
            val p1: (String?) -> Boolean = { it == null || it.isEmpty() }
            val p2: Predicate<String> = Predicate<String>(p1) // Noncompliant {{Use "(T) -> Boolean" instead.}}
            val p3: Predicate<String> = Predicate<String> { it == null || it.isEmpty() } // Noncompliant {{Use "(T) -> Boolean" instead.}}
            val p4 = createPredicate() // Compliant, no issues on inferred types

            val f1: Function<Int, String>? = null // Noncompliant {{Use "(T) -> R" instead.}}
            val s1: Supplier<Int>? = null // Noncompliant {{Use "() -> T" instead.}}
            val s2: (() -> Int)? = null // Compliant
        }

        private fun createPredicate(): Predicate<String>? = null // Noncompliant {{Use "(T) -> Boolean" instead.}}
    }

    fun join() {
        val list = listOf("a", "b", "c")
        val mutableList = mutableListOf("a", "b", "c")
        val array = arrayOf(1, 2, 3)
        val map = mapOf("a" to 1, "b" to 2, "c" to 3)

        println(com.google.common.base.Joiner.on('-').join(list)) // Noncompliant {{Use "Iterable<T>.joinToString" instead.}}
        //                                            ^^^^
        println(list.joinToString("-")) // Compliant

        println(com.google.common.base.Joiner.on('-').join(mutableList)) // Noncompliant {{Use "Iterable<T>.joinToString" instead.}}
        //                                            ^^^^
        println(mutableList.joinToString("-")) // Compliant

        println(com.google.common.base.Joiner.on(",").join(array)) // Noncompliant {{Use "Array<T>.joinToString" instead.}}
        //                                            ^^^^
        println(array.joinToString(","))

        println(com.google.common.base.Joiner.on('-').join(list.iterator())) // Compliant, no replacement

        println(com.google.common.base.Joiner.on(' ').withKeyValueSeparator(":").join(map)) // Compliant, no replacement
    }

    fun tmpDir() {
        val f1 = com.google.common.io.Files.createTempDir() // Noncompliant {{Use "kotlin.io.path.createTempDirectory" instead.}}
        val f2 = kotlin.io.path.createTempDirectory() // Compliant
    }

    fun immutableSetMapListOf() {
        val s1 = ImmutableSet.of("a", "b") // Noncompliant {{Use "kotlin.collections.setOf" instead.}}
        val s2 = setOf("a", "b") // Compliant

        val l1 = ImmutableList.of("a", "b") // Noncompliant {{Use "kotlin.collections.listOf" instead.}}
        val l2 = listOf("a", "b") // Compliant

        val m1 = ImmutableMap.of("a", "b") // Noncompliant {{Use "kotlin.collections.mapOf" instead.}}
        val m2 = mapOf("a" to "b") // Compliant
    }

    fun base64() {
        val b1 = com.google.common.io.BaseEncoding.base64() // Noncompliant {{Use "java.util.Base64" instead.}}
        val b2 = java.util.Base64.getEncoder() // Compliant

        val u1 = com.google.common.io.BaseEncoding.base64Url() // Noncompliant {{Use "java.util.Base64" instead.}}
        val u2 = java.util.Base64.getUrlEncoder() // Compliant
    }

    fun optional(p: Optional<String>) { // Noncompliant {{Use "java.util.Optional" instead.}}
        //          ^^^^^^^^^^^^^^^^
        val o1 = Optional.of("") // Noncompliant {{Use "java.util.Optional.of" instead.}}
        //                ^^
        val o2 = Optional.absent<String>() // Noncompliant {{Use "java.util.Optional.empty" instead.}}
        //                ^^^^^^
        val o3 = Optional.fromNullable<String>(null) // Noncompliant {{Use "java.util.Optional.ofNullable" instead.}}
        //                ^^^^^^^^^^^^
    }

    fun nestedFunctionPropertyAndParameter() {

        class Nested {

            val property
                get() = Optional.of("") // Noncompliant
            //                   ^^

            fun nested(p: Optional<String>) { // Noncompliant
                //        ^^^^^^^^^^^^^^^^
            }

        }
    }
}
