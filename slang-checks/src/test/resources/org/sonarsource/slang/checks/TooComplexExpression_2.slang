  a && b;
  a && b || c;
  a && b || c && d; // Noncompliant {{Reduce the number of conditional operators (3) used in the expression (maximum allowed 2).}}
