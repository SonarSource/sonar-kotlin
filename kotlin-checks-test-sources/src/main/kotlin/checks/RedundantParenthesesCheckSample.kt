package checks

class RedundantParenthesesCheckSample {

    val a = ((1)) // Noncompliant {{Remove these useless parentheses.}}
//           ^ ^<

    val b = (1)

}
