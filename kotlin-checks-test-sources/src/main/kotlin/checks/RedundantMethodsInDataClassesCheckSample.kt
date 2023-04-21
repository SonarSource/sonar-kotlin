package checks

import java.util.*

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
}
