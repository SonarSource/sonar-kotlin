package checks

class ReportCharacterClassRegexDummyCheckSample {
    fun characterClasses() {

        Regex("[a-z]abc[A-Z]abc[b-d]abc[0-9]") // Noncompliant {{Character class found}}
//      ^^^^^> ^^^^^   ^^^^^<  ^^^^^<  ^^^^^<


        """[0-9]x\nxx[a-z]x\nxx[_-z]""".toRegex() // Noncompliant {{Character class found}}
//         ^^^^^     ^^^^^<    ^^^^^<   ^^^^^^^<

    }
}
