match (x) { 1 -> a; else -> b; }; // OK

match (x) {
    1 -> a;
    2 -> match (y) { // Noncompliant {{Refactor the code to eliminate this nested "match".}}
    //   ^^^^^
      3 -> c;
      else -> d;
    };
    else -> b;
};

match (x) {
    1 -> a;
    2 -> {
      match (y) { // Noncompliant {{Refactor the code to eliminate this nested "match".}}
          3 -> c;
          else -> d;
       };
       match (z) { // Noncompliant {{Refactor the code to eliminate this nested "match".}}
          3 -> c;
          else -> d;
       };
    };
    else -> b;
};

match (x) {
    1 -> a;
    2 -> match (y) { // Noncompliant {{Refactor the code to eliminate this nested "match".}}
      3 -> c;
      else -> match (z) { // Noncompliant {{Refactor the code to eliminate this nested "match".}}
        4 -> d;
        else -> e;
      };
    };
    else -> b;
};

match (x) {
    1 -> a;
    2 -> b;
    else -> match (y) { // Noncompliant {{Refactor the code to eliminate this nested "match".}}
      3 -> c;
      else -> d;
    };
};