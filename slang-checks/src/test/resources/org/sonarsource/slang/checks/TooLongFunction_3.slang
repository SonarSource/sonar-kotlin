fun f1() {
  a; a;
}

fun f1() { // Noncompliant {{This function has 4 lines of code, which is greater than the 3 authorized. Split it into smaller functions.}}
//  ^^
  a;
  a;
}

fun f1() {

  a;
}

fun f1() {
  // comment
  a;
}

int fun foo( // Compliant, no line of code
p1,
p2,
p3,
p4)
{
}

int fun foo(
p1,
p2,
p3,
p4);

int fun (
p1,
p2,
p3,
p4);

    int fun () // Noncompliant
//  ^^^^^^^^^^
{
  a;
  a;
  a;
  a;
}
