package checks

class ClearTextProtocolInTestFileSample {
    fun `clear-text urls in a test file are fixtures, not findings`() {
        val api = "http://api.acme.com/v1/users" // Compliant - test file
        val files = "ftp://files.acme.com/data" // Compliant - test file
        val legacy = "telnet://legacy.acme.com" // Compliant - test file
        val socket = "ws://socket.acme.com/live" // Compliant - test file
    }
}
