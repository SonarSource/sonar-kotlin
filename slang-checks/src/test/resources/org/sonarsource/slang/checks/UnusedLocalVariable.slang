int var global;       // Compliant

void fun fooBar() {
    int val a = 0;    // Compliant

    int val b;        // Noncompliant {{Remove this unused "b" local variable.}}
    //      ^

    int var c;        // Noncompliant {{Remove this unused "c" local variable.}}
    //      ^

    int var d;        // Compliant
    d = 0;

    int var e;        // Compliant
    e = d + a;
}
