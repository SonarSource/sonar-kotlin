package checks

import okhttp3.ConnectionSpec
import okhttp3.TlsVersion
import java.security.NoSuchAlgorithmException
import java.security.NoSuchProviderException
import javax.net.ssl.SSLContext

class WeakSSLContextCheck {
    fun okHttp(argumentVersion: String) {
        val spec: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_0) // Noncompliant {{Change this code to use a stronger protocol.}}
            .build()
        val spec2: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_1) // Noncompliant {{Change this code to use a stronger protocol.}}
            .build()
        val spec3: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_2) // Compliant
            .build()
        val spec4: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3) // Compliant
            .build()
        val spec5: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.SSL_3_0) // Compliant
            .build()
        val specWithString: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions("TLSv1") // Noncompliant
            .build()
        val specWithString2: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions("TLSv1.1") // Noncompliant
            .build()
        val specWithString3: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions("TLSv1.2") // Compliant
            .build()
        val specWithMultipleVersions: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_1) // Noncompliant
            .build()
        val specWithMultipleWeakVersions: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(
                TlsVersion.TLS_1_0,  // Noncompliant
                TlsVersion.TLS_1_1
            )
            .build()
        val specWithMultipleWeakVersions2: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(
                "TLSv1",  // Noncompliant
                "TLSv1.1"
            )
            .build()
        val specWithUnknownValue: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(argumentVersion) // Compliant, unknown version
            .build()
        val specWithUnknownValue2: ConnectionSpec = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
            .tlsVersions(getVersion()) // Compliant, unknown version
            .build()
    }

    fun getVersion(): String {
        return "TLSv1.1"
    }

    private val PROTOCOL = "SSL"

    fun foo(protocol: String?, provider: String?) {
        bar(SSLContext.getInstance(protocol))
        bar(SSLContext.getInstance("SSL")) // Noncompliant {{Change this code to use a stronger protocol.}}
        bar(SSLContext.getInstance("SSLv2")) // Noncompliant
        bar(SSLContext.getInstance("SSLv3")) // Noncompliant
        bar(SSLContext.getInstance("TLS")) // Noncompliant
        bar(SSLContext.getInstance("TLSv1")) // Noncompliant
        bar(SSLContext.getInstance("TLSv1.1")) // Noncompliant
        bar(SSLContext.getInstance("TLSv1.2"))
        bar(SSLContext.getInstance("TLSv1.3"))
        bar(SSLContext.getInstance("DTLS")) // Noncompliant
        bar(SSLContext.getInstance("DTLSv1.0")) // Noncompliant
        bar(SSLContext.getInstance("DTLSv1.2"))
        bar(SSLContext.getInstance("DTLSv1.3"))
        bar(SSLContext.getInstance("SSL", provider)) // Noncompliant
        bar(SSLContext.getInstance("TLSv1.2", provider))
        bar(SSLContext.getInstance("TLSv1.2", "SSL"))
        bar(SSLContext.getInstance(getVersion()))
        bar(SSLContext.getInstance(String.format(getVersion())))
        bar(SSLContext.getInstance(PROTOCOL)) // Noncompliant
    }

    fun bar(ctx: SSLContext?) {
        println(ctx)
    }

}
