package checks

class EqualsOverriddenWithArrayFieldCheckSample {
    data class PersonWithoutEqualsHashcodeAndToString( // Noncompliant {{Override equals, hashCode and toString to consider array content in the method.}}
        val names: Array<String>, val age: Int
    ) {}

    data class PersonWithoutEqualsAndToString( // Noncompliant {{Override equals and toString to consider array content in the method.}}
        val names: Array<String>,
        val age: Int
    ) {
        override fun hashCode(): Int {
            var result = names.contentHashCode()
            result = 31 * result + age
            return result
        }
    }

    data class PersonWithoutHashCodeAndToString( // Noncompliant {{Override hashCode and toString to consider array content in the method.}}
        val names: Array<String>,
        val age: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PersonWithoutHashCodeAndToString

            if (!names.contentEquals(other.names)) return false
            if (age != other.age) return false

            return true
        }
    }

    data class PersonWithoutEqualsAndHashCode( // Noncompliant {{Override equals and hashCode to consider array content in the method.}}
        val names: Array<String>,
        val age: Int
    ) {
        override fun toString(): String {
            return super.toString()
        }
    }

    data class WithoutEquals(val names: Array<String>, val age: Int) { // Noncompliant {{Override equals to consider array content in the method.}}

        override fun hashCode(): Int {
            var result = names.contentHashCode()
            result = 31 * result + age
            return result
        }

        override fun toString(): String {
            return super.toString()
        }
    }

    data class WithoutHashCode(val names: Array<String>, val age: Int) { // Noncompliant {{Override hashCode to consider array content in the method.}}

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as WithoutHashCode

            if (!names.contentEquals(other.names)) return false
            if (age != other.age) return false

            return true
        }

        override fun toString(): String {
            return super.toString()
        }
    }

    data class WithoutToString(val names: Array<String>, val age: Int) { // Noncompliant {{Override toString to consider array content in the method.}}

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as WithoutToString

            if (!names.contentEquals(other.names)) return false
            if (age != other.age) return false

            return true
        }

        override fun hashCode(): Int {
            var result = names.contentHashCode()
            result = 31 * result + age
            return result
        }
    }

    data class WithInBodyProperty(val age: Int) { // Noncompliant {{Override equals and hashCode to consider array content in the method.}}
        val names: Array<String> = arrayOf("Alice")
        override fun toString(): String {
            return "$names\n$age"
        }
    }

    abstract class AmbiguousParent {
        abstract fun equals(other: String?): Boolean

        abstract fun hashCode(ignored: Any?): Int

        abstract fun toString(returned: String): String
    }

    data class AmbiguousOverrides(val names: Array<String>, val age: Int) : AmbiguousParent() { // Noncompliant {{Override equals, hashCode and toString to consider array content in the method.}}

        override fun equals(other: String?): Boolean {
            return true
        }

        override fun hashCode(ignored: Any?): Int {
            return 42
        }

        override fun toString(returned: String): String {
            return returned
        }
    }

    data class Person(val names: Array<String>, val age: Int) { // Compliant
        fun double() = age * 2

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Person

            if (!names.contentEquals(other.names)) return false
            if (age != other.age) return false

            return true
        }

        override fun hashCode(): Int {
            var result = names.contentHashCode()
            result = 31 * result + age
            return result
        }

        override fun toString(): String {
            return super.toString()
        }
    }

    data class WithoutBody(val names: Array<String>) // Noncompliant {{Override equals, hashCode and toString to consider array content in the method.}}

    data class EmptyBody(val names: Array<String>) { // Noncompliant {{Override equals, hashCode and toString to consider array content in the method.}}
    }

    data class ArrayInBody(val age: Int) { // Noncompliant {{Override equals, hashCode and toString to consider array content in the method.}}
        val employers = arrayOf("SonarSource")
    }

    data class NoArray(val age: Int) { // Compliant
        val employer = "SonarSource"
    }

    data class NoArrayEmptyBody(val age: Int) { // Compliant
    }

    data class NoArrayOrBody(val age: Int) // Compliant
}
