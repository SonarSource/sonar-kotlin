package checks

import java.util.regex.Pattern

class UnicodeAwareCharClassesCheckSample1 {
    fun NoncompliantCharRanges() {
        val regex = "[a-zA-Z]" // Noncompliant
//                   ^^^^^^^^
//                    ^^^@-1< {{Character range}}
//                       ^^^@-2< {{Character range}}
        Pattern.compile(regex + regex)
//              ^^^^^^^< {{Function call of which the argument is interpreted as regular expression.}}
    }
}
