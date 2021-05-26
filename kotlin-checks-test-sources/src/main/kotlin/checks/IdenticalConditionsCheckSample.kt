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

    }
}
