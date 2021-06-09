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

        if (a != 2) { };
        (i >= 10)

        !(!b)
        -(-i)

        // TODO false-negatives
        !(i is Number)
        !(i === 10)
        !(i !== 10)
    }
}
