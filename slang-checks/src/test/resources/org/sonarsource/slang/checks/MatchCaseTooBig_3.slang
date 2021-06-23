match (x) {
    1 -> { // Noncompliant {{Reduce this case clause number of lines from 4 to at most 3, for example by extracting code into methods.}}
//  ^^^^
        a = 1;
        print(a);
    };
    else -> b;
};

match (x) {
    1 -> foo();
    else -> { // Noncompliant {{Reduce this case clause number of lines from 4 to at most 3, for example by extracting code into methods.}}
//  ^^^^^^^
        a = 1;
        print(a);
    };
};

match (x) {
    1 -> foo(); // OK
    // comments should be ignored
    // another comment

    else -> b;
};
