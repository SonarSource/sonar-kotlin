package checks

import java.util.regex.Pattern

class UnicodeAwareCharClassesCheckSample {
    fun NoncompliantCharRanges() {

        Pattern.compile("[a-z]") // Noncompliant {{Replace this character range with a Unicode-aware character class.}}
//                        ^^^
        Pattern.compile("[A-Z]") // Noncompliant
        Regex("[A-Z]") // Noncompliant {{Replace this character range with a Unicode-aware character class.}}
        Pattern.compile("[0-9a-z]") // Noncompliant
        "[0-9a-z]".toRegex() // Noncompliant {{Replace this character range with a Unicode-aware character class.}}
        Pattern.compile("[abcA-Zdef]") // Noncompliant
        Pattern.compile("[\\x{61}-\\x{7A}]") // Noncompliant
//                        ^^^^^^^^^^^^^^^
        Pattern.compile("[a-zA-Z]") // Noncompliant {{Replace these character ranges with Unicode-aware character classes.}}
//                       ^^^^^^^^
        val regex = "[a-zA-Z]" // Noncompliant
//                   ^^^^^^^^
//                    ^^^@-1< {{Character range}}
//                       ^^^@-2< {{Character range}}
        Pattern.compile(regex + regex)
//              ^^^^^^^< {{Function call of which the argument is interpreted as regular expression.}}
    }

    fun NoncompliantPredefinedPosixClasses() {
        Pattern.compile("\\p{Lower}") // Noncompliant {{Enable the "UNICODE_CHARACTER_CLASS" flag or use a Unicode-aware alternative.}}
//              ^^^^^^^  ^^^^^^^^^^<

        Regex("\\p{Lower}") // Noncompliant {{Enable the "U" flag or use a Unicode-aware alternative.}}
        Pattern.compile("\\p{Alnum}") // Noncompliant
        Pattern.compile("\\p{Space}") // Noncompliant
        Pattern.compile("\\s") // Noncompliant
        Pattern.compile("\\S") // Noncompliant
        Pattern.compile("\\w") // Noncompliant
        Pattern.compile("\\W") // Noncompliant
        Pattern.compile("\\s\\w\\p{Lower}") // Noncompliant
        Pattern.compile("\\S\\p{Upper}\\w") // Noncompliant
    }

    fun compliantCharRanges() {
        Pattern.compile("[0-9]") // Compliant: we do not consider digits
        Pattern.compile("[a-y]") // Compliant: It appears a more restrictive range than simply 'all letters'
        Pattern.compile("[D-Z]")
        Pattern.compile("[\\x{1F600}-\\x{1F637}]")
    }

    fun compliantPredefinedPosixClasses() {

        Regex("""\p{IsAlphabetic}""")
        Regex("""\p{IsLatin}""") // matches latin letters, including umlauts and other non-ASCII variations
        Regex("""(?U)\p{Alpha}""")
        Regex("(?U)\\p{Alpha}")

        Pattern.compile("\\p{ASCII}")
        Pattern.compile("\\p{Cntrl}")
        Pattern.compile("\\p{Lower}", Pattern.UNICODE_CHARACTER_CLASS)
        Pattern.compile("(?U)\\p{Lower}")
        Pattern.compile("\\w", Pattern.UNICODE_CHARACTER_CLASS)
        Pattern.compile("(?U)\\w")
        Pattern.compile("(?U:\\w)")
        Pattern.compile("\\w", Pattern.CANON_EQ or Pattern.COMMENTS or Pattern.UNICODE_CHARACTER_CLASS or Pattern.UNIX_LINES)
        Pattern.compile("\\w((?U)\\w)\\w")
        Pattern.compile("\\w(?U:[a-y])\\w") // Compliant. We assume the developer knows what they are doing if they are using unicode flags somewhere.
    }
}
