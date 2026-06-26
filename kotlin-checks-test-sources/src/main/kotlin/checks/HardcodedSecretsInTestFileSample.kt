package checks

// No "Noncompliant" markers: these secrets would be reported in main code,
// but the check is expected to stay silent when the file is classified as a test file.
internal class HardcodedSecretsInTestFileSample {
    fun f() {
        val mySecret = "abcdefghijklmnopqrs"
        val params = "login=a&secret=abcdefghijklmnopqrs"
    }
}
