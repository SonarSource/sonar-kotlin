package checks

class RedundantTypeCastsCheckSample {
    fun foo() {
        val i: Int = 10

        // already Int
        val n1 = i as Int // Noncompliant {{Remove this useless cast.}}

        // downcasting
        val n2 = i as Number // Compliant

        // TODO FN in K2
//        i as Number // Noncompliant

        // always false
        if (i !is Int) { // Noncompliant {{Remove this useless `is` check.}}
        }

        // always true
        if (i is Int) { // Noncompliant
        }

        val s: Any = ""
        if (s is String) {
            // Smart cast
            val s1 = s as String // Noncompliant
        }

        // Redundant after explicit type declaration
        // TODO FN in K2
//        val n3: Number = 5 as Number // Noncompliant
    }

}
