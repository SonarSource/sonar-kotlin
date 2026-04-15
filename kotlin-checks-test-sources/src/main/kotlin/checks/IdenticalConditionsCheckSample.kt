package checks

class IdenticalConditionsCheckSample {
    fun f(x: Boolean, y: Boolean, z: Boolean, i: Int, a: String, b: String) {
        // if

        if (x) {
        } else if (x) { // Noncompliant {{This condition duplicates the one on line 7.}}
        }

        if (x) {}
        if (x) {} else {};
        if (x) {} else if (y) {};
        if (x) {} else if (x) {}; // Noncompliant
//          ^>             ^
        if (x) {} else if (y) {} else if (z) {};
        if (x) {} else if (y) {} else if (y) {}; // Noncompliant
//                         ^>             ^
        if (x) {} else if (x) {} else if (x) {}; // Noncompliant 2
        if (x) {} else if (y) {} else if ((y)) {}; // Noncompliant


// match

        when (i) { 1 -> a; };
        when (i) { 1 -> a; else -> b; };
        when (i) { 1 -> a; 2 -> b; };
        when (i) { 1 -> a; 1 -> b; }; // Noncompliant
//                 ^>      ^
        when (i) { 1 -> a; (1) -> b; } // Noncompliant

// when with guard conditions

        when (x) {
            true if a == "foo" -> "1"
            true if a == "bar" -> "2" // Compliant: different guard conditions
            true -> "3" // Compliant: follows guarded entries with same pattern
            false -> "4"
        }

        when (x) {
            true if a == "foo" -> "1"
            true if a == "foo" -> "2" // Noncompliant {{This condition duplicates the one on line 42.}}
            else -> "3"
        }

        when (x) {
            true if a == "foo" -> "1"
            false -> "2"
            true if a == "bar" -> "3" // Compliant: same pattern but different guard
            else -> "4"
        }

        when (i) {
            1 if a == "foo" -> "a"
            1 if a == "bar" -> "b" // Compliant: different guard conditions
            1 -> "c" // Compliant: no guard vs guarded
            2 -> "d"
        }

        when (i) {
            1 if a == "x" -> "a"
            2 -> "b"
            1 if a == "x" -> "c" // Noncompliant {{This condition duplicates the one on line 62.}}
        }

    }
}
