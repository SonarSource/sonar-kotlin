package checks

class EqualsOverriddenWithArrayFieldCheckSampleNoSemantics {

    data class PersonWithoutEqualsOrHashcode( // Compliant - FN  without semantics
        val names: Array<String>, val age: Int
    ) {}

    data class PersonWithoutEquals( // Compliant - FN  without semantics
        val names: Array<String>,
        val age: Int
    ) {
        override fun hashCode(): Int {
            var result = names.contentHashCode()
            result = 31 * result + age
            return result
        }
    }

    data class PersonWithoutHashCode(  // Compliant - FN  without semantics
        val names: Array<String>,
        val age: Int
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PersonWithoutHashCode

            if (!names.contentEquals(other.names)) return false
            if (age != other.age) return false

            return true
        }
    }

    data class Person(val names: Array<String>, val age: Int) { // Compliant
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as PersonWithoutHashCode

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

    data class WithInBodyProperty(val age: Int) {  // Compliant - FN  without semantics
        val names: Array<String> = arrayOf("Alice")
        override fun toString(): String {
            return "$names\n$age"
        }
    }

    data class ArraylessClass(val age: Int) { // Compliant
    }
}
