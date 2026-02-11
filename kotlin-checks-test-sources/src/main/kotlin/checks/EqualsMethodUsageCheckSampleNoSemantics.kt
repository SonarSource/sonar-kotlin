package checks

class EqualsMethodUsageCheckSampleNoSemantics {

    fun test(str1: String, str2: String?) {
        // This SHOULD be flagged - standard 1-arg equals can be replaced with ==
        if (str1.equals(str2)) { // Noncompliant {{Replace "equals" with binary operator "==".}}
//               ^^^^^^
        }

        // These should NOT be flagged - equals with ignoreCase is NOT equivalent to ==
        // Regression test for SONARKT-259
        if (str1.equals(str2, ignoreCase = true)) { // Compliant
        }
        if (str1.equals(str2, ignoreCase = false)) { // Compliant
        }
        if (str1.equals(str2, true)) { // Compliant
        }
        if (!str1.equals(str2, ignoreCase = true)) { // Compliant
        }
    }
}
