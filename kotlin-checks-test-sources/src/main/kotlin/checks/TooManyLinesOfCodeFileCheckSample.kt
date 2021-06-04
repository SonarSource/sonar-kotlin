// Noncompliant@0 {{File has 8 lines, which is greater than 1 authorized. Split it into smaller files.}}
package checks

class TooManyLinesOfCodeFileCheckSample {

    fun multilineStringLiteral() =
        """
            First line
            Second line
        """

}
