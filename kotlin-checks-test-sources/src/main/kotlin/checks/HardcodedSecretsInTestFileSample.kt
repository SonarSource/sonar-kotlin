package checks

// These values would trigger S6418 in main code but are suppressed when the file is a test file.
internal class HardcodedSecretsInTestFileSample {
    fun f() {
        val mySecret = "Hj4pZ9wLdN2sKq7VtXy"
        println("login=a&secret=Hj4pZ9wLdN2sKq7VtXy")
        println(mySecret)
    }
}
