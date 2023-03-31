package checks

class EqualsMethodUsageCheckSample {

    fun test(arg1: String, arg2: String?, arg3: Int, arg4: String?) {
        if (arg1.equals(arg2)) { // Noncompliant {{Replace "equals" with binary operator "==".}}
//               ^^^^^^
        }
        if (!arg1.equals(arg2)) { // Noncompliant {{Replace "!" and "equals" with binary operator "!=".}}
//                ^^^^^^
//          ^@-1< {{Negation}}
        }
        if (this.equals(arg2)) { // Noncompliant {{Replace "equals" with binary operator "==".}}
//               ^^^^^^
        }
        if (super.equals(arg2)) { // Compliant, (super == arg2) is not equivalent and does not compile
        }
        if (equals(arg2)) { // Compliant, do not ask to replace by "this == arg2"
        }
        if (this.equals(arg3)) { // Compliant, does not match "equals(other: Any?)" but "equals(other: Int)"
        }
        if (arg1 == arg2) { // Compliant
        }
        if (arg1 != arg2) { // Compliant
        }
        if (equals(arg2).not()) { // Compliant, equals is on the left side of KtDotQualifiedExpression
        }
        if (arg2?.equals(arg4) ?: (arg4 === null)) { // Compliant, this condition match exactly the generated logic behind "arg2 == arg4"
                                                     // but this rule does not target KtSafeQualifiedExpression
        }
        // with unsupported unary operator
        if (-arg1.equals(arg2)) { // Noncompliant {{Replace "equals" with binary operator "==".}}
//                ^^^^^^
        }
        if (!(arg1.equals(arg2))) { // Noncompliant
//          ^>     ^^^^^^
        }
        if (!((arg1.equals(arg2)))) { // Noncompliant
//          ^>      ^^^^^^
        }
        if ((
                arg1.equals(arg3) // Noncompliant
//                   ^^^^^^
            ).equals( // Noncompliant
//            ^^^^^^
                arg3.equals(arg1) // Noncompliant
//                   ^^^^^^
            )) {
            // nested equals equivalent to (arg1 == arg3) == (arg3 == arg1)
        }
    }

    fun equals(other: Int) = this.toString() == other.toString()

    operator fun Boolean.unaryMinus() = not()

    class MyOuterClass {
        inner class MyInnerClass {
            fun test(arg: String) {
                if (this.equals(arg)) { } // Noncompliant
                if (this@MyOuterClass.equals(arg)) { } // Compliant, (this@MyOuterClass == arg) is not equivalent and does not compile
            }
        }
    }
}
