package sources.kotlin

class S2068 {

    fun foo() {
        val params = "user=admin&password=Password123"   // Sensitive
        val sqlserver = "pgsql:host=localhost port=5432 dbname=test user=postgres password=postgres"   // Sensitive
    }

    fun f1() {
        val password = "Password"                 // Compliant
    }
    fun f2() {
        val password = "[id='password']"          // Compliant
    }
    fun f3() {
        val password = "custom.password"          // Compliant
    }
    fun f4() {
        val password = "trustStorePassword"       // Compliant
    }
    fun f5() {
        val password = "connection.password"      // Compliant
    }
    fun f6() {
        val password = "/users/resetUserPassword" // Compliant
    }

    fun databaseQuery(password : String) {
      val query1 = "password=?"                  // Compliant
      val query2 = "password=:password"          // Compliant
      val query3 = "password=:param"             // Compliant
      val query4 = "password='" + password + "'" // Compliant
      val query5 = "password=%s"                 // Compliant
      val query6 = "password=%v"                 // Compliant
    }

    fun uriUserInfo() {
      val url1 = "scheme://user:azerty123@domain.com"  // Sensitive
      val url2 = "scheme://user:@domain.com"           // Compliant
      val url3 = "scheme://user@domain.com:80"         // Compliant
      val url4 = "scheme://user@domain.com"            // Compliant
      val url5 = "scheme://domain.com/user:azerty123"  // Compliant
    }
}
