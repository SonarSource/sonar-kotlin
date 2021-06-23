void fun FOOBAR() {}  // OK
void fun foo() {} // Noncompliant {{Rename function "foo" to match the regular expression ^[A-Z]*$}}
//       ^^^

class A {
  fun constructor() {} // Compliant, constructor
}
