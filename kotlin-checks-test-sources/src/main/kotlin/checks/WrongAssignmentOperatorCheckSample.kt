package checks

class WrongAssignmentOperatorCheckSample {
    fun f() {
        var num = -1
        var target = 0
        target =-num // Noncompliant {{Was "-=" meant instead?}}
//             ^^
        target =
            -num
        target = -num // Compliant intent to assign inverse value of num is clear
        target =--num

        target += num
        target =+ num // Noncompliant {{Was "+=" meant instead?}}
//             ^^
        target =
            + num
        target =
            +num
        target = +num
        target =++num
        target=+num // Compliant - no spaces between variable, operator and expression

        var a = true
        var b = false
        var c = true

        a = b != c;

        a =! c; // Noncompliant {{Add a space between "=" and "!" to avoid confusion.}}
        a = ! c;
        a = !c;
        a =
            !c;

    }
}
