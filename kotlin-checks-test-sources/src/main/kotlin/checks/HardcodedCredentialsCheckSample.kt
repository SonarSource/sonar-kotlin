package checks

class HardcodedCredentialsCheckSample {
    fun f() {

        var x = "pass"
        "pass"
        x = "password"
        "password"
        x = "login=a&password="
        "login=a&password="
        val value = ""
        "login=a&password= " + value
        "login=a&password=Rb7kZpQ2" // Noncompliant
        x = "login=a&password=Rb7kZpQ2" // Noncompliant {{"password" detected here, make sure this is not a hard-coded credential.}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^
        "login=a&password=Rb7kZpQ2" // Noncompliant
        "login=a&passwd=Rb7kZpQ2" // Noncompliant {{"passwd" detected here, make sure this is not a hard-coded credential.}}
        "login=a&pwd=Rb7kZpQ2" // Noncompliant {{"pwd" detected here, make sure this is not a hard-coded credential.}}
        "login=a&passphrase=Rb7kZpQ2" // Noncompliant {{"passphrase" detected here, make sure this is not a hard-coded credential.}}
        var variableNameWithPasswordInIt = "Rb7kZpQ2" // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        var variableNameWithPasswdInIt = "Rb7kZpQ2" // Noncompliant
        variableNameWithPasswdInIt += "Rb7kZpQ2" // Noncompliant
        var variableNameWithPwdInIt = "Rb7kZpQ2"  // Noncompliant {{"Pwd" detected here, make sure this is not a hard-coded credential.}}

        A("").variableNameWithPwdInIt = "Rb7kZpQ2" // Noncompliant

        val constValue = "login=a&password=Rb7kZpQ2" // Noncompliant
        var passwd = "Rb7kZpQ2" // Noncompliant
        var passphrase = "Rb7kZpQ2" // Noncompliant
        var okVariable = "Rb7kZpQ2"
        B.variableNameWithPwdInIt = ""
        passwd = ""

// No issue is raised when the matched wordlist item is present in both symbol name and literal string value.
        var password = "password" // Compliant
        var myPassword = "users/connection.secretPassword" // Compliant
        myPassword = "users/connection.secretPassword" // Compliant
        myPassword = "secretPasswd" // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
        myPassword = "secretPasswd" // Noncompliant {{"Password" detected here, make sure this is not a hard-coded credential.}}
        var params = "user=admin&password=Rb7kZpQ2" // Noncompliant {{"password" detected here, make sure this is not a hard-coded credential.}}

// Values recognized as non-secrets by the shared SecretClassifier are not reported.
        var dbPassword = "<placeholder>" // Compliant, placeholder

// Database queries are compliant
        var query = "password=?"
        query = "password=:password"
        query = "password=:param"
        query = "password='" + password + "'"
        query = "password=%s"
        query = "password=%v"

// String format is compliant
        query = "password={0}"

// Support URI
        var uri = "http://user:azer:ty123@domain.com" // Noncompliant
        uri = "https://:azerty123@domain.com/path" // Noncompliant {{Review this hard-coded URL, which may contain a credential.}}
        uri = "http://anonymous:anonymous@domain.com" // Compliant, user and password are the same
        uri = "http://user:@domain.com"
        uri = "http://user@domain.com:80"
        uri = "http://domain.com/user:azerty123"
        uri = "too-long-url-scheme://user:123456@server.com"
        uri = "https:// invalid::url::format"

    }

    private fun userObject(user: User): String {
        // Compliant
        return """
            |{
            |  username="${user.username}"
            |  password="${user.password}"
            |  permissions=[${user.permissions.joinToString(", ")}]
            |}
            """.trimMargin()
    }
}

data class User(val username: String, val password: String, val permissions: List<String>)

class A(var variableNameWithPwdInIt: String)

object B {
    var variableNameWithPwdInIt: String = "Rb7kZpQ2" // Noncompliant
}
