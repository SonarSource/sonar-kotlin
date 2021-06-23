  match (x) {
    1 -> 'a';
    2 -> 'b';
    3 -> 'c';
  };

  match (x) { // Noncompliant {{Reduce the number of match branches from 4 to at most 3.}}
    1 -> 'a';
    2 -> 'b';
    3 -> 'c';
    4 -> 'd';
  };

  match (x) {
    1 -> 'a';
    2 -> 'b';
    else -> 'c';
  };

  match (x) { // Noncompliant
//^^^^^
    1 -> 'a';
//  ^^^^<
    2 -> 'b';
//  ^^^^<
    3 -> 'c';
//  ^^^^<
    else -> 'd';
//  ^^^^^^^<
};
