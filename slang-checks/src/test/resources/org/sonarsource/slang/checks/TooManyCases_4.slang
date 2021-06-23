match (x) {
  1 -> 'a';
  2 -> 'b';
  3 -> 'c';
  4 -> 'd';
};

match (x) { // Noncompliant
  1 -> 'a';
  2 -> 'b';
  3 -> 'c';
  4 -> 'd';
  5 -> 'e';
};
