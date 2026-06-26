package checks

// No "Noncompliant" markers: these credentials would be reported in main code,
// but the check is expected to stay silent when the file is classified as a test file.
internal class HardcodedCredentialsInTestFileSample {
    fun f() {
        val params = "user=admin&password=Password123"
        var passwd = "xxxx"
        passwd = "yyyy"
    }
}
