print("Hello World!"); print("Hello World!"); //Noncompliant {{Reformat the code to have only one statement per line.}}

if (a) {}; if (b) {}; // Noncompliant
//         ^^^^^^^^^

fun foo() {} fun foo() {} // Noncompliant
//           ^^^^^^^^^^^^

fun foo() {
  if (a) {}; if (b) {}; // Noncompliant
//           ^^^^^^^^^
}

if (a) {
  a(); b(); // Noncompliant
//     ^^^
};

a(); b(); c(); // Noncompliant
//   ^^^  ^^^<

if(a) { foo(); // FN
  b = 2;
};

val a = 1; val b = 1; // Noncompliant

match (x) {
 1 -> {
   a(); b(); // Noncompliant
 };
 else -> print(1);
};

fun foo() { print(); } // OK

fun foo() { print();
  foo();
} // OK

if (a) { b; }; // OK