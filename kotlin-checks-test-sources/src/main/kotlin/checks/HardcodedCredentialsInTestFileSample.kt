package checks

// These values would trigger S2068 in main code but are suppressed when the file is a test file.
internal class HardcodedCredentialsInTestFileSample {
    fun f() {
        println("login=a&password=Rb7kZpQ2")
        var passwd = "Rb7kZpQ2"
        println(passwd)
        passwd = "Rb7kZpQ2"
        println(passwd)
    }
}
