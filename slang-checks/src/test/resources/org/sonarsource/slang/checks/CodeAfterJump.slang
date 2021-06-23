fun testReturn() {
  return 42; // Noncompliant {{Refactor this piece of code to not have any dead code after this "return".}}
//^^^^^^^^^
  42;
}

fun lastReturn() {
  42;
  return 42;
}

fun testBreak() {
  while (cond) {
    break; // Noncompliant {{Refactor this piece of code to not have any dead code after this "break".}}
//  ^^^^^
    42;
    42;
  }
}

fun testContinue() {
  while (cond) {
    continue; // Noncompliant {{Refactor this piece of code to not have any dead code after this "continue".}}
//  ^^^^^^^^
    42;
  }
}

fun testContinueWithLabel() {
  while (cond) {
    continue myLabel; // Noncompliant {{Refactor this piece of code to not have any dead code after this "continue".}}
//  ^^^^^^^^^^^^^^^^
    42;
  }
}

fun empty() {
}

fun fnRequiresCfg() {
  if (cond) {
    return;
  } else {
    return;
  };

  42;
}

fun testThrow() {
  throw 42; // Noncompliant {{Refactor this piece of code to not have any dead code after this "throw".}}
  42;
}
