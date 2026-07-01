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
        val variable2 = "login=a&secret=Hj4pZ9wLdN2sKq7VtXy" // Noncompliant {{"secret" detected here, make sure this is not a hard-coded secret.}}
        //              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        val variable3 = "login=a&token=Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val variable4 = "login=a&api_key=Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val variable5 = "login=a&api.key=Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val variable6 = "login=a&api-key=Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val variable7 = "login=a&credential=Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val variable8 = "login=a&auth=Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val variable9 = "login=a&secret="
        val variableA = "login=a&secret= "
        val variableB = "secret=&login=Hj4pZ9wLdN2sKq7VtXy" // Compliant
        val variableC = "Okapi-key=42, Okapia Johnstoni, Forest/Zebra Giraffe" // Compliant
        val variableD = "gran-papi-key=Known by everybody in the world like PWD" // Compliant
        // Noncompliant@+1
        val variableE = """
      login=a
      secret=Hj4pZ9wLdN2sKq7VtXy

      """.trimIndent()
        // Noncompliant@+2
        // Noncompliant@+1
        val variableF = """
      <form action="/delete?secret=Hj4pZ9wLdN2sKq7VtXy">
        <input type="text" id="item" value="42"><br><br>
        <input type="submit" value="Delete">
      </form>
      <form action="/update?api-key=Hj4pZ9wLdN2sKq7VtXy">
        <input type="text" id="item" value="42"><br><br>
        <input type="submit" value="Update">
      </form>

      """.trimIndent()

        // Secrets starting with "?", ":", "\"", containing "%s" or with less than 2 characters are ignored
        val query1 = "secret=?Hj4pZ9wLdN2sKq7VtXy" // Compliant
        val query1_1 = "secret=???" // Compliant
        val query1_2 = "secret=X" // Compliant
        val query1_3 = "secret=anonymous" // Compliant
        val query4 = "secret='" + secret + "'" // Compliant
        val query2 = "secret=:password" // Compliant
        val query3 = "secret=:param" // Compliant
        val query5 = "secret=%s" // Compliant
        val query6 = "secret=\"%s\"" // Compliant
        val query7 = "\"secret=\"" // Compliant

        val params1 = "user=admin&secret=Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val params2 = "secret=no\nuser=admin0123456789" // Compliant
        val sqlserver1 =
            "pgsql:host=localhost port=5432 dbname=test user=postgres secret=Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val sqlserver2 = "pgsql:host=localhost port=5432 dbname=test secret=no user=Hj4pZ9wLdN2sKq7VtXy" // Compliant

        // Spaces and & are not included into the token, it shows us the end of the token.
        val params3 = "token=Hj4pZ9wLdN2sKq7VtXy user=admin" // Noncompliant
        val params4 = "token=Hj4pZ9wLdN2sKq7VtXy&user=admin" // Noncompliant

        val params5 =
            "token=abc&Hj4pZ9wLdN2sKq7VtXy" // Compliant, FN, even if "&" is accepted in a password, it also indicates a cut in a string literal
        val params6 = "token=Hj4pZ9wLd:N2sKq7VtXy" // Noncompliant

        // URLs are reported by S2068 only.
        val urls = arrayOf<String?>(
            "http://user:123456@server.com/path",  // Compliant
        )

        // ========== 2. Variable declaration ==========
        // The variable name should contain a secret word
        val MY_SECRET = "Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val variableNameWithSecretInIt = "Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val variableNameWithSecretaryInIt = // Noncompliant
            "Hj4pZ9wLdN2sKq7VtXy"
        val variableNameWithAuthorshipInIt = // Noncompliant
            "Hj4pZ9wLdN2sKq7VtXy"
        val variableNameWithTokenInIt = "Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val variableNameWithApiKeyInIt = "Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val variableNameWithCredentialInIt = "Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        val variableNameWithAuthInIt = "Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        // Secrets with less than 2 characters, explicitly "anonymous", are ignored
        val variableNameWithSecretInItEmpty = ""
        val variableNameWithSecretInItOneChar = "X"
        val variableNameWithSecretInItAnonymous = "anonymous"
        var otherVariableNameWithAuthInIt: String?

        // Secret containing words and random characters should be filtered
        val secret001 = "sk_live_xf2fh0Hu3LqXlqqUg2DEWhEz" // Noncompliant
        val secret777 = "sk_live_aaaaaaaaaaaaaaaaaaaaaaaa" // Compliant, not enough entropy
        val secret003 = "commits/8f3b7d1e5a9c2f6b4d0e7a" // Noncompliant
        val secret004 = "examples/commit/revision/469001e9700fea0"
        val secret006 = "Xk9Lm2Qp7Rs4Tv8W" // Compliant
        val secret007 = "Hj4pZ9wLdN2sKq7Vt" // Noncompliant
        val secret008 = "9f3b7d1e5a8c2f6b4" // Noncompliant
        val secret009 = "907481352690748135269074813526" // Noncompliant
        val secret010 = "Hj4pZ9wLdN2sKq7VtXyKm3Rn8Qp" // Noncompliant
        val secret011 = "748903526174890352617489035261" // Noncompliant
        val secret012 = "639182047563918204756391820475" // Noncompliant
        val secret013 = "234.167.076.123"
        val secret015 = "org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH"
        // Example of Telegram bot token
        val secret016 = "bot907481:ABD-EFG9wghIkl-zyx57W2v1u907ew11" // Noncompliant
        // Secret with "&"
        val secret017 = "907&481352690748135269074&813526" // Noncompliant
        val secret018 = "&97&481352690748135269074&81352&" // Noncompliant

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

        // Values recognized as non-secrets by the shared SecretClassifier are filtered even though
        // they have enough length and entropy to otherwise be reported.
        val mySecretEnc = "enc[Hj4pZ9wLdN2sKq7VtXyKm3]" // Compliant, encrypted marker
        val mySecretPath = "/etc/svc/conf/Hj4pZ9wLdN2sKq7VtXy" // Compliant, filesystem path

        // = in the middle or end is okay
        val secretWithBackSlashes8 = "Hj4pZ9wLdN2sKq7Vt=" // Noncompliant
        val secretWithBackSlashes9 = "Hj4pZ9wLdN2sKq7Vt==" // Noncompliant
        val secretWithBackSlashes10 = "Hj4pZ9wL=dN2sKq7Vt" // Noncompliant

        // Only [a-zA-Z0-9_.+/~$-] are accepted as secrets characters
        val OkapiKeyboard = "what a strange QWERTY keyboard for animals" // Compliant
        val OKAPI_KEYBOARD = "what a strange QWERTY keyboard for animals" // Compliant
        val okApiKeyValue = "Spaces are UNEXPECTED 012 345 678" // Compliant
        val tokenism = "(Queen's Partner's Stored Knowledge is a Minimal Sham)" // Compliant

        // ========== 3. Assignment ==========
        fieldNameWithSecretInIt = "Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        this.fieldNameWithSecretInIt = "Hj4pZ9wLdN2sKq7VtXy" // Noncompliant
        // Secrets with less than 2 chars are explicitly ignored
        fieldNameWithSecretInIt = "X"
        // "anonymous" is explicitly ignored
        fieldNameWithSecretInIt = "anonymous"
        // Not hardcoded
        fieldNameWithSecretInIt = retrieveSecret()
        this.fieldNameWithSecretInIt = retrieveSecret()
        variable1 = "Hj4pZ9wLdN2sKq7VtXy"
    }

    private fun getSecret(s: String?): CharArray? {
        return null
    }

    private fun retrieveSecret(): String? {
        return null
    }

    companion object {
        private const val PASSED = "Hj4pZ9wLdN2sKq7VtXy" // compliant nothing to do with secrets
        private const val EMPTY = ""
    }
}
