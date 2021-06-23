void fun foobar() {}
int fun foo_bar() {}  // Compliant - has no line

fun smallF1() {
  foo = 1;
  bar = foo > 3 || bar
}

fun smallF2() { // Noncompliant
  foo = 1;
  bar = foo > 3 || bar
}

string fun f1() {
//         ^^>
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

int fun f2() {
  foobar = "abc";
  foo = 1;
  baz = foo > 3 || bar
}

boolean fun f3() { // Noncompliant {{Update this function so that its implementation is not identical to "f1" on line 14.}}
//          ^^
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

fun f4() { // Noncompliant {{Update this function so that its implementation is not identical to "f1" on line 14.}}
//  ^^
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

fun f5(a) { // Compliant - different parameter list
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

fun f6() {
  foo = 1;
}

fun f7() { // Compliant - only 1 line
  foo = 1;
}

fun f8(int a) { // Compliant
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

fun f9(int a) { // Noncompliant
  foobar = "abc"; foo = 1; bar = foo > 3 || bar
}

fun f10(string a) { // Compliant - not same parameter type
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

fun(int a, int b) { // Compliant - not same parameters
  foobar = "abc";
  foo = 1;
  bar = foo > 3 || bar
}

fun f11(int a, int b) {
  foobar = "abcdefg";
  foo = 1;
  bar = foo > 3 || bar
}

fun f12(int a, int b) {
  foobar = "abc";
  foo = 2;
  bar = foo > 3 || bar
}

class A {

  fun constructor(int a) {
    foo = 1;
    bar = 2;
  }

  fun constructor(int a) { // Compliant, constructor
    foo = 1;
    bar = 2;
  }

}
