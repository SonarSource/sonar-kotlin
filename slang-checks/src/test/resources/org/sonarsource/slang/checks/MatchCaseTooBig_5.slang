match (x) {
    expression -> { // Noncompliant {{Reduce this case clause number of lines from 8 to at most 5, for example by extracting code into methods.}}
 // ^^^^^^^^^^^^^
        a = 1;
        foo();
        bar();
        if (a == 1) {
          print(1);
        };
    };
    else -> b;
};

match (x) {
    1 -> foo(); // OK
    else -> b;
};
