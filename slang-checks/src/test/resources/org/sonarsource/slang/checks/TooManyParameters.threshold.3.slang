int fun foo(p1, p2, p3) {}
int fun foo(p1, p2, p3, p4, p5, p6, p7, p8) {} // Noncompliant {{This function has 8 parameters, which is greater than the 3 authorized.}}
//      ^^^             ^^< ^^< ^^< ^^< ^^<
