fun ok() {
  if (x) {
    if (y) {
      foo();
    };
  };
  if (z) {
    foo();
  };
}

fun ko() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 5 to the 4 allowed.}} [[effortToFix=1]]
//  ^^
  if (x) {
//^^< {{+1}}
    if (y) {
//  ^^< {{+2 (incl 1 for nesting)}}
      foo();
    };
    if (z) {
//  ^^< {{+2 (incl 1 for nesting)}}
      foo();
    };
  };
}

fun logical_operators() { // Noncompliant
//  ^^^^^^^^^^^^^^^^^
  if (a
//^^<
     && b && c
//   ^^<
     || d || e
//   ^^<
     && f
//   ^^<
     || g) {
//   ^^<
    foo();
  }
}

fun nesting_anonymous() { // Noncompliant
  fun() {
    a && b || c && d || e && f;
  }
}

