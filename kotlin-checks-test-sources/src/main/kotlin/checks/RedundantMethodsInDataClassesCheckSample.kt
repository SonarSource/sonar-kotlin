package checks

import java.util.*

class RedundantMethodsInDataClassesCheckSample {

    data class Person1(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Noncompliant {{Remove this redundant method which is the same as a default one.}}
//                   ^^^^^^
            return other is Person1 && other.name == name && other.age == age
        }

        override fun hashCode() // Noncompliant {{Remove this redundant method which is the same as a default one.}}
//                   ^^^^^^^^
                = Objects.hash(name, age)
    }

    data class Person2(val name: String, val age: Int) // Compliant

    data class Person3(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Compliant
            return other is Person3 && other.name.lowercase() == name.lowercase() && other.age == age
        }

        override fun hashCode() = Objects.hash(name.lowercase(), age) // Compliant
    }

    data class Person4(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Noncompliant
            return other is Person4 && name == other.name && age == other.age
        }

        override fun hashCode() = Objects.hash(name, age) // Noncompliant
    }

    data class Person5(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Compliant
            return other is Person5 && other.name == name && age.dec() == age || a()
        }

        fun a() = false

        override fun hashCode() = Objects.hash(age, name) + Objects.hash(name, age)// Compliant
    }

    data class Person6(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Compliant
            return other is Person6 && other.name == name && other.age == age && 1 != 1
        }

        override fun hashCode() = Objects.hash(name.lowercase(), age.dec()) // Compliant
    }

    data class Person7(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Compliant
            return other is Person7 && other.name == name
        }

        override fun hashCode() = Objects.hash(4) // Compliant
    }

    data class Person8(val name: String, val age: Int) {
        val a = 5
    }

    data class Person9(val name: String, val age: Int) { // Compliant
        fun equals(other: Any?, a: Int, b: Int): Boolean { // Compliant
            return other is Person7 && other.name == name
        }

        fun hashCode(a: Int) = Objects.hash() // Compliant
    }

    data class Person10(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Compliant
            return other is Person10 && age == 2 && name.lowercase().uppercase() == other.name
        }

        override fun hashCode() = Objects.hash(name.lowercase(), age.dec()) // Compliant
    }

    data class Person11(val name: String, val age: Int) {
        val a = 1
        val b = ""
        override fun equals(other: Any?): Boolean { // Compliant
            return other is Person11 && other.b == name && other.age == age
        }

        override fun hashCode() = Objects.hash(1, a.dec()) // Compliant
    }

    data class Person12(val name: String, val age: Int) {
        val a = 1
        val b = ""
        override fun equals(other: Any?): Boolean = other is Person12 && other.b == name && other.age == age


        override fun hashCode() = hash() // Compliant

        private fun hash(): Int {
            return 1
        }

    }

    data class Person13(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Compliant
            val x = other is Person13 && other.name == name && other.age == age
            return true
        }

        override fun hashCode(): Int { // Compliant
            val x = Objects.hash(name, age)
            if (true) {
                return x + 1
            }
            return Objects.hash(name, age)
        }
    }

    data class Person14(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Compliant
            if (true) {
                return other is Person14 && other.name == name && other.age == age
            } else {
                return other is Person14 && other.name == name && other.age == age
            }
            val x = other is Person14 && other.name == name && other.age == age
            return true
        }

        override fun hashCode(): Int { // Compliant
            return 31 * name.hashCode() + Integer.hashCode(age)
        }
    }

//    data class Person15(val name: String, val age: Int) {
//        override fun equals(other: Any?): Boolean { // Compliant
//            return other is Person1 && other.name == name && other.age == age
//        }
//
//        override fun hashCode() = Arrays.hashCode(arrayOf(name, age)) // Noncompliant
//    }

    data class Person16(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Compliant
            return true
        }

        override fun hashCode() = Arrays.hashCode(arrayOf(name)) // Compliant
    }

    data class Person17(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Compliant
            return other !is Person1
        }

        override fun hashCode() = Arrays.hashCode(arrayOf(name)) // Compliant
    }

    data class Person18(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Compliant
            return other is Person1 && other is Person2
        }

        override fun hashCode() = Arrays.hashCode(arrayOf(name)) // Compliant
    }

    data class Person19(val name: String, val age: Int) {
        val a = ""
        override fun equals(other: Any?) = true // Compliant

        override fun hashCode() = Arrays.hashCode(arrayOf(a)) // Compliant
    }

    data class Person20(val name: String, val age: Int) {
        override fun equals(other: Any?) = true // Compliant

        override fun hashCode() = name.hashCode() // Compliant
    }

    data class Person21(val name: String, val age: Int) {
        val a = 3
        override fun equals(other: Any?): Boolean { // Compliant
            return other !is Person1
        }

        override fun hashCode() = this.a + a // Compliant
    }

//    data class Person22(val name: String, val age: Int) {
//        val a = arrayOf(name, age)
//
//        override fun hashCode() = Arrays.hashCode(a) // FN
//    }

}
