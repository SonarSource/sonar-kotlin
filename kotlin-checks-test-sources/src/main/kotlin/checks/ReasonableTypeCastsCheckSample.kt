package checks

class ReasonableTypeCastsCheckSample {
    fun noncompliant() {
        val i: Int = 10

        // Throws ClassCastException
        val s1a: String = i as String // Noncompliant {{Remove this cast that can never succeed.}}
        val s1b = i as String // Noncompliant

        // Will always be null
        val s2: String? = i as? String // Noncompliant

        val list = listOf(1, 2, 3, 4)

        // any operation with list elements will produce ClassCastException
        val strings1 = list as List<String> // Noncompliant {{Remove this unchecked cast.}}

        // any operation with list elements will produce ClassCastException
        val strings2 = list as? List<String> // Noncompliant
    }

    fun compliant() {
        val i = 10
        val s1 = i as Number

        val list = listOf(1, 2, 3, 4)

        // any operation with list elements will produce ClassCastException
        val strings1 = list as List<Number>
    }
}
