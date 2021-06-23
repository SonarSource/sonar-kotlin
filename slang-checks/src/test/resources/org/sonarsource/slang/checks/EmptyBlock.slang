void fun a() {}

void fun b() {
  // comment
}

fun () {
  // comment
}

fun () {
  // Noncompliant@+1
  if (x > 0) { };
}

void fun c(int x) {
  // Noncompliant@+1
  if (x > 0) { };

  if (x > 1) {
    // comment
  };

  match (x) {
  // comment
  };

  // Noncompliant@+1
  match (x) {

  };

  match (x) {
    // Noncompliant@+1
    1 -> { };
    2 -> x;
    else -> { x; };
  };

  while (cond) {} // Compliant - exception to the rule
}