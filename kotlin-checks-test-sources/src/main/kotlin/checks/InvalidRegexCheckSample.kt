package checks

import java.util.regex.Pattern

class InvalidRegexCheckSample {
    private fun noncompliant() {
        Regex("(") // Noncompliant {{Expected ')', but found the end of the regex}}
//      ^^^^^> {{Function call of which the argument is interpreted as regular expression.}}
//              ^@-1 1
        (((("(")))).toRegex() // Noncompliant {{Expected ')', but found the end of the regex}}
//            ^
        Regex("""(""") // Noncompliant {{Expected ')', but found the end of the regex}}
//                ^
        Regex("x{1,2,3}|(") // Noncompliant {{Fix the syntax errors inside this regex.}}
//      ^^^^^       ^< {{Expected '}', but found ','}}
//                       ^@-1< {{Expected ')', but found the end of the regex}}
        Regex("(\\w+-(\\d+)") // Noncompliant {{Expected ')', but found the end of the regex}}
        Regex("[") // Noncompliant {{Expected ']', but found the end of the regex}}

        // Noncompliant@+1 {{Expected ')', but found the end of the regex}}
        val const = "foo"
//                      ^
        Regex("(" + const)
//      ^^^^^<
    }
    private fun compliant(arg: String) {
        Regex("(" + arg) // Compliant - we don't know what arg is

        Regex("")
        Regex("a")
        Regex("[a-z]")
        Pattern.compile("\\(\\[")
        Pattern.compile("([", Pattern.LITERAL)
        Regex("\\(\\[")
        Regex("([", RegexOption.LITERAL)

        val str = "(["
        str.replace("([", "{")

        Regex("a|b")

        Regex("()")

        str.replace("abc".toRegex(), "x")
        str.replace("x{42}".toRegex(), "x")

        Regex("(\\w+)-(\\d+)")
        // Errors in backreferences are handled by rule S6001
        // Errors in backreferences are handled by rule S6001
        Regex("(\\w+)-\\2")
        Regex("(?<name>\\w+)-\\k<name>")
    }
}
