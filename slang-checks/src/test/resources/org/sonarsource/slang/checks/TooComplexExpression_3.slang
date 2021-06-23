  a && b;
  a && b || c;
  a && b || c && d;
  a && b || c && d || e; // Noncompliant {{Reduce the number of conditional operators (4) used in the expression (maximum allowed 3).}} [[effortToFix=1]]
  a && b || c && d || e && f; // Noncompliant [[effortToFix=2]]
//^^^^^^^^^^^^^^^^^^^^^^^^^^
if (a && b || c && d || e && f) {}; // Noncompliant
if (a && (b || c) && (d || e && f)) {}; // Noncompliant
if (a && b || c) {};
if (!(a && b || c && d)) {};
if (!(a && b || c && d || e)) {}; // Noncompliant
foo(a && b) && foo(a || b) && foo(a && b);
