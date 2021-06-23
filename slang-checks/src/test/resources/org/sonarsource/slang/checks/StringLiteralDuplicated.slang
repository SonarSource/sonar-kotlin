x = "string literal1";                           // Noncompliant {{Define a constant instead of duplicating this literal "string literal1" 3 times.}} [[effortToFix=2]]
//  ^^^^^^^^^^^^^^^^^
x += "string literal1" + "other string literal";
//  <^^^^^^^^^^^^^^^^^
native[] { [] } = native[] {
  [
    "string literal1"
// <^^^^^^^^^^^^^^^^^
  ]
};

void fun function1(string abcde) {
  v = "string literal2" + "string literal2"     // Compliant - literal only appears twice
}
"string literal3"; "string literal3";
"string literal3${x}";                          // Compliant - string entries of string templates not considered as string literals

void fun funtcion2(int abcde) {
  if (abcde == "string literal4") {             // Noncompliant {{Define a constant instead of duplicating this literal "string literal4" 5 times.}} [[effortToFix=4]]
//             ^^^^^^^^^^^^^^^^^
  }
}

match("string literal4") {
//   <^^^^^^^^^^^^^^^^^
    1 -> "string literal4";
//      <^^^^^^^^^^^^^^^^^
    "string literal4" -> "string literal4";
// <^^^^^^^^^^^^^^^^^   <^^^^^^^^^^^^^^^^^
};

"abcd"; "abcd"; "abcd"; "abcd";                 // Compliant - string length smaller than threshold
"string_literal5"; "string_literal5";
"string_literal5"; "string_literal5";           // Compliant - single word