// if

if (x) {
} else if (x) { // Noncompliant {{This condition duplicates the one on line 3.}}
};

if (x) {};
if (x) {} else {};
if (x) {} else if (y) {};
if (x) {} else if (x) {}; // Noncompliant
//  ^>             ^
if (x) {} else if (y) {} else if (z) {};
if (x) {} else if (y) {} else if (y) {}; // Noncompliant
//                 ^>             ^
if (x) {} else if (x) {} else if (x) {}; // Noncompliant 2
if (x) {} else if (y) {} else if ((y)) {}; // Noncompliant


// match

match (x) { 1 -> a; };
match (x) { 1 -> a; else -> b; };
match (x) { 1 -> a; 2 -> b; };
match (x) { 1 -> a; 1 -> b; }; // Noncompliant
//          ^>      ^
match (x) { 1 -> a; (1) -> b; }; // Noncompliant
