package checks

class ReasonableTypeCastsCheckSample {
    fun noncompliant() {
        val i1: Int = 10
        // Throws ClassCastException
        val s1: String = i1 as String // Noncompliant {{Remove this cast that can never succeed.}}

        val i2: Int = 10
        // Throws ClassCastException
        val s2 = i2 as String // Noncompliant {{Remove this cast that can never succeed.}}

        val i3: Int = 10
        // Will always be null
        val s3: String? = i3 as? String // Noncompliant {{Remove this cast that can never succeed.}}

        val list = listOf(1, 2, 3, 4)
        // any operation with list elements will produce ClassCastException
        val strings1 = list as List<String> // Noncompliant {{Remove this unchecked cast.}}

        val list2 = listOf(1)
        // any operation with list elements will produce ClassCastException
        val strings2 = list2 as? List<String> // Noncompliant {{Remove this unchecked cast.}}
    }

    fun compliant() {
        val i = 10
        val s1 = i as Number

        val list = listOf(1, 2, 3, 4)

        // any operation with list elements will produce ClassCastException
        val strings1 = list as List<Number>
    }
}
