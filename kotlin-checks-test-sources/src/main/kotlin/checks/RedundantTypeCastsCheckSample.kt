package checks

class RedundantTypeCastsCheckSample {
    fun foo() {
        val i: Int = 10

        // already Int
        val n1 = i as Int // Noncompliant {{Remove this useless cast.}}

        // downcasting
        val n2 = i as Number // Compliant

        // TODO not found after previous finding on i
        i as Number // Noncompliant

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
        // TODO found after replacement of Number by Int
        val n3: Number = 5 as Number // Noncompliant
    }

    // TODO from RSPEC
    fun types(value: Int, elements: List<Number>) {
        val a: Number = value as Number // Noncompliant
        val b: Number? = value as? Number // Noncompliant
    }


}
