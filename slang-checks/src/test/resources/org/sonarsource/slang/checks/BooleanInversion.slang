if (!(a == 2)) { };  // Noncompliant {{Use the opposite operator ("!=") instead.}}
//  ^^^^^^^^^
!(i < 10);  // Noncompliant {{Use the opposite operator (">=") instead.}}
!(i > 10);  // Noncompliant {{Use the opposite operator ("<=") instead.}}
!(i != 10);  // Noncompliant {{Use the opposite operator ("==") instead.}}
!(i <= 10);  // Noncompliant {{Use the opposite operator (">") instead.}}
!(i >= 10);  // Noncompliant {{Use the opposite operator ("<") instead.}}

if (a != 2) { };
(i >= 10);
!(a + i);