package checks

/**
 * This check detect hardcoded secrets in multiples cases:
 * - 1. String literal
 * - 2. Variable declaration
 * - 3. Assignment
 */
internal class HardCodedSecretCheckSample {
    var fieldNameWithSecretInIt: String? = retrieveSecret()

    private fun a(secret: CharArray?, `var`: String?) {
        // ========== 1. String literal ==========
        // The variable name does not influence the issue, only the string is considered.
        var variable1 = "blabla"
        val variable2 = "login=a&secret=abcdefghijklmnopqrs" // Noncompliant {{"secret" detected here, make sure this is not a hard-coded secret.}}
        //              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        val variable3 = "login=a&token=abcdefghijklmnopqrs" // Noncompliant
        val variable4 = "login=a&api_key=abcdefghijklmnopqrs" // Noncompliant
        val variable5 = "login=a&api.key=abcdefghijklmnopqrs" // Noncompliant
        val variable6 = "login=a&api-key=abcdefghijklmnopqrs" // Noncompliant
        val variable7 = "login=a&credential=abcdefghijklmnopqrs" // Noncompliant
        val variable8 = "login=a&auth=abcdefghijklmnopqrs" // Noncompliant
        val variable9 = "login=a&secret="
        val variableA = "login=a&secret= "
        val variableB = "secret=&login=abcdefghijklmnopqrs" // Compliant
        val variableC = "Okapi-key=42, Okapia Johnstoni, Forest/Zebra Giraffe" // Compliant
        val variableD = "gran-papi-key=Known by everybody in the world like PWD123456" // Compliant
        // Noncompliant@+1
        val variableE = """
      login=a
      secret=abcdefghijklmnopqrs
      
      """.trimIndent()
        // Noncompliant@+2
        // Noncompliant@+1
        val variableF = """
      <form action="/delete?secret=abcdefghijklmnopqrs">
        <input type="text" id="item" value="42"><br><br>
        <input type="submit" value="Delete">
      </form>
      <form action="/update?api-key=abcdefghijklmnopqrs">
        <input type="text" id="item" value="42"><br><br>
        <input type="submit" value="Update">
      </form>
      
      """.trimIndent()

        // Secrets starting with "?", ":", "\"", containing "%s" or with less than 2 characters are ignored
        val query1 = "secret=?abcdefghijklmnopqrs" // Compliant
        val query1_1 = "secret=???" // Compliant
        val query1_2 = "secret=X" // Compliant
        val query1_3 = "secret=anonymous" // Compliant
        val query4 = "secret='" + secret + "'" // Compliant
        val query2 = "secret=:password" // Compliant
        val query3 = "secret=:param" // Compliant
        val query5 = "secret=%s" // Compliant
        val query6 = "secret=\"%s\"" // Compliant
        val query7 = "\"secret=\"" // Compliant

        val params1 = "user=admin&secret=Secret0123456789012345678" // Noncompliant
        val params2 = "secret=no\nuser=admin0123456789" // Compliant
        val sqlserver1 =
            "pgsql:host=localhost port=5432 dbname=test user=postgres secret=abcdefghijklmnopqrs" // Noncompliant
        val sqlserver2 = "pgsql:host=localhost port=5432 dbname=test secret=no user=abcdefghijklmnopqrs" // Compliant

        // Spaces and & are not included into the token, it shows us the end of the token.
        val params3 = "token=abcdefghijklmnopqrs user=admin" // Noncompliant
        val params4 = "token=abcdefghijklmnopqrs&user=admin" // Noncompliant

        val params5 =
            "token=123456&abcdefghijklmnopqrs" // Compliant, FN, even if "&" is accepted in a password, it also indicates a cut in a string literal
        val params6 = "token=123456:abcdefghijklmnopqrs" // Noncompliant

        // URLs are reported by S2068 only.
        val urls = arrayOf<String?>(
            "http://user:123456@server.com/path",  // Compliant
        )

        // ========== 2. Variable declaration ==========
        // The variable name should contain a secret word
        val MY_SECRET = "abcdefghijklmnopqrs" // Noncompliant
        val variableNameWithSecretInIt = "abcdefghijklmnopqrs" // Noncompliant
        val variableNameWithSecretaryInIt = // Noncompliant
            "abcdefghijklmnopqrs"
        val variableNameWithAuthorshipInIt = // Noncompliant
            "abcdefghijklmnopqrs"
        val variableNameWithTokenInIt = "abcdefghijklmnopqrs" // Noncompliant
        val variableNameWithApiKeyInIt = "abcdefghijklmnopqrs" // Noncompliant
        val variableNameWithCredentialInIt = "abcdefghijklmnopqrs" // Noncompliant
        val variableNameWithAuthInIt = "abcdefghijklmnopqrs" // Noncompliant
        // Secrets with less than 2 characters, explicitly "anonymous", are ignored
        val variableNameWithSecretInItEmpty = ""
        val variableNameWithSecretInItOneChar = "X"
        val variableNameWithSecretInItAnonymous = "anonymous"
        var otherVariableNameWithAuthInIt: String?

        // Secret containing words and random characters should be filtered
        val secret001 = "sk_live_xf2fh0Hu3LqXlqqUg2DEWhEz" // Noncompliant
        val secret777 = "sk_live_aaaaaaaaaaaaaaaaaaaaaaaa" // Compliant, not enough entropy
        val secret003 = "examples/commit/8e1d746900f5411e9700fea0" // Noncompliant
        val secret004 = "examples/commit/revision/469001e9700fea0"
        val secret006 = "abcdefghijklmnop" // Compliant
        val secret007 = "abcdefghijklmnopq" // Noncompliant
        val secret008 = "0123456789abcdef0" // Noncompliant
        val secret009 = "012345678901234567890123456789" // Noncompliant
        val secret010 = "abcdefghijklmnopabcdefghijkl" // Noncompliant
        val secret011 = "012345670123456701234567012345" // Noncompliant
        val secret012 = "012345678012345678012345678012" // Noncompliant
        val secret013 = "234.167.076.123"
        val secret015 = "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH"
        // Example of Telegram bot token
        val secret016 = "bot123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11" // Noncompliant
        // Secret with "&"
        val secret017 = "012&345678012345678012345&678012" // Noncompliant
        val secret018 = "&12&345678012345678012345&67801&" // Noncompliant

        // Simple constants will be filtered thanks to the entropy check
        val SECRET_INPUT = "[id='secret']" // Compliant
        val SECRET_PROPERTY = "custom.secret" // Compliant
        val TRUSTSTORE_SECRET = "trustStoreSecret" // Compliant
        val CONNECTION_SECRET = "connection.secret" // Compliant
        val RESET_SECRET = "/users/resetUserSecret" // Compliant
        val RESET_TOKEN = "/users/resetUserToken" // Compliant
        val secretToChar = "secret".toCharArray() // Compliant
        val secretToChar2 = "http-secret".toCharArray() // Compliant
        val secretToString = "http-secret".toString() // Compliant
        val secretFromGetSecret = getSecret("") // Compliant

        val CA_SECRET = "ca-secret" // Compliant
        val caSecret = CA_SECRET // Compliant

        // = in the middle or end is okay
        val secretWithBackSlashes8 = "abcdefghijklmnopqrs=" // Noncompliant
        val secretWithBackSlashes9 = "abcdefghijklmnopqrs==" // Noncompliant
        val secretWithBackSlashes10 = "abcdefghij=klmnopqrs" // Noncompliant

        // Only [a-zA-Z0-9_.+/~$-] are accepted as secrets characters
        val OkapiKeyboard = "what a strange QWERTY keyboard for animals" // Compliant
        val OKAPI_KEYBOARD = "what a strange QWERTY keyboard for animals" // Compliant
        val okApiKeyValue = "Spaces are UNEXPECTED 012 345 678" // Compliant
        val tokenism = "(Queen's Partner's Stored Knowledge is a Minimal Sham)" // Compliant

        // ========== 3. Assignment ==========
        fieldNameWithSecretInIt = "abcdefghijklmnopqrs" // Noncompliant
        this.fieldNameWithSecretInIt = "abcdefghijklmnopqrs" // Noncompliant
        // Secrets with less than 2 chars are explicitly ignored
        fieldNameWithSecretInIt = "X"
        // "anonymous" is explicitly ignored
        fieldNameWithSecretInIt = "anonymous"
        // Not hardcoded
        fieldNameWithSecretInIt = retrieveSecret()
        this.fieldNameWithSecretInIt = retrieveSecret()
        variable1 = "abcdefghijklmnopqrs"
    }

    private fun getSecret(s: String?): CharArray? {
        return null
    }

    private fun retrieveSecret(): String? {
        return null
    }

    companion object {
        private const val PASSED = "abcdefghijklmnopqrs" // compliant nothing to do with secrets
        private const val EMPTY = ""
    }
}

