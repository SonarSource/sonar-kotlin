package checks

import java.util.Objects

class RedundantMethodsInDataClassesCheckSample {

    data class Person1(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Noncompliant
            return other is Person1 && other.name == name && other.age == age
        }

        override fun hashCode() = Objects.hash(name, age) // Noncompliant
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
            return other is Person5 && other.name == name && age.dec() == age
        }

        override fun hashCode() = Objects.hash(age, name) // Noncompliant
    }

    data class Person6(val name: String, val age: Int) {
        override fun equals(other: Any?): Boolean { // Compliant
            return other is Person6 && other.name == name && age.dec() == age
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

}
