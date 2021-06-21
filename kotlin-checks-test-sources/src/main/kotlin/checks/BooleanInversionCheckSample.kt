package checks

class BooleanInversionCheckSample {
    fun f(a: Int, i: Int, b: Boolean) {
        if (!(a == 2)) { }  // Noncompliant {{Use the opposite operator ("!=") instead.}}
//          ^^^^^^^^^
        !(i < 10)  // Noncompliant {{Use the opposite operator (">=") instead.}}
        !(i > 10)  // Noncompliant {{Use the opposite operator ("<=") instead.}}
        !(i != 10)  // Noncompliant {{Use the opposite operator ("==") instead.}}
        !(i <= 10)  // Noncompliant {{Use the opposite operator (">") instead.}}
        !(i >= 10)  // Noncompliant {{Use the opposite operator ("<") instead.}}
        !(i === 10) // Noncompliant {{Use the opposite operator ("!==") instead.}}
        !(i !== 10) // Noncompliant {{Use the opposite operator ("===") instead.}}
        !(i is Number) // Noncompliant {{Use the opposite operator ("!is") instead.}}
        !(i !is Number) // Noncompliant {{Use the opposite operator ("is") instead.}}

        if (a != 2) { };
        (i >= 10)

        !(!b)
        -(-i)
    }
}
