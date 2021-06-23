void fun fooBar() {}  // OK
fun () {}             // OK
void fun foo_bar() {} // Noncompliant {{Rename function "foo_bar" to match the regular expression ^[a-z][a-zA-Z0-9]*$}}
//       ^^^^^^^
