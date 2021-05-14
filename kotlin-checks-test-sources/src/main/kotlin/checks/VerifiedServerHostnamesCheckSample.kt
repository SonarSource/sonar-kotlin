package checks

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLSession
import okhttp3.OkHttpClient

class VerifiedServerHostnamesCheckSample {

    companion object {
        val CONSTANT_TRUE = true
        val CONSTANT_FALSE = false
    }

    fun f() {
        var t = true
        val builder = OkHttpClient.Builder()
        builder.hostnameVerifier(object : HostnameVerifier {
            override fun verify(hostname: String?, session: SSLSession?): Boolean { // Noncompliant
                return true
            }
        })
        builder.hostnameVerifier(object : HostnameVerifier {
            override fun verify(hostname: String?, session: SSLSession?): Boolean {
                return false
            }
        })
        builder.hostnameVerifier(object : HostnameVerifier {
            override fun verify(hostname: String?, session: SSLSession?): Boolean {
                return CONSTANT_FALSE
            }
        })
        builder.hostnameVerifier(object : HostnameVerifier {
            override fun verify(hostname: String?, session: SSLSession?): Boolean {  // Compliant, not a constant
                return t
            }
        })
        builder.hostnameVerifier(object : HostnameVerifier {
            override fun verify(hostname: String?, session: SSLSession?): Boolean { // Compliant
                if(something()) {
                    return false
                } else {
                    return true
                }
            }
        })
        builder.hostnameVerifier(object : HostnameVerifier {
            override fun verify(hostname: String?, session: SSLSession?): Boolean { // Noncompliant
                return CONSTANT_TRUE
            }
        })
        builder.hostnameVerifier { hostname, session -> true } // Noncompliant
        builder.hostnameVerifier { _, _ -> true } // Noncompliant
        builder.hostnameVerifier { hostname, session -> CONSTANT_TRUE } // Noncompliant
        builder.hostnameVerifier { _, _ -> CONSTANT_TRUE } // Noncompliant
        builder.hostnameVerifier { hostname, session -> false } // Compliant
        builder.hostnameVerifier { _, _ -> CONSTANT_FALSE } // Compliant
        builder.hostnameVerifier { _, _ -> t } // Compliant, not a constant

        this.hostnameVerifier { hostname, session -> true } // Compliant, custom function
    }
    
    fun hostnameVerifier(verifier: HostnameVerifier) {
        TODO()
    }

    private fun something(): Boolean {
        TODO("Not yet implemented")
    }
}
