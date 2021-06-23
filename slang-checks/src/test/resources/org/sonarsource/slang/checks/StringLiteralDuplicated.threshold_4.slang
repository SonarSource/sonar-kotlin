  "string literal1";  "string literal1";  "string literal1"; // Compliant - only appears 3 times which is less than the custom threshold of 4
  "string literal2"; // Noncompliant {{Define a constant instead of duplicating this literal "string literal2" 4 times.}}
//^^^^^^^^^^^^^^^^^
   "string literal2";  "string literal2";  "string literal2";
//<^^^^^^^^^^^^^^^^^  <^^^^^^^^^^^^^^^^^  <^^^^^^^^^^^^^^^^^
