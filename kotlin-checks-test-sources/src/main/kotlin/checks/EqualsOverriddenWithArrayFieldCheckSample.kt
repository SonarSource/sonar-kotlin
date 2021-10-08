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

    data class WithoutBody(val names: Array<String>) // Noncompliant {{Override equals, hashCode and toString to consider array content in the method.}}


    data class Person(val names: Array<String>, val age: Int) { // Compliant
        fun double() = age * 2

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PersonWithoutHashCodeAndToString

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

    data class NoArray(val age: Int) { // Compliant
    }

    data class NoArrayOrBody(val age: Int) // Compliant
}
