package checks

import java.net.Socket
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

class ServerCertificateCheckSample {
}

internal class TrustAllManager : X509TrustManager {
    @Throws(CertificateException::class)
    override fun checkClientTrusted( // Noncompliant {{Enable server certificate validation on this SSL/TLS connection.}}
        x509Certificates: Array<X509Certificate>,
        s: String,
    ) { 
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
        LOG.log(Level.SEVERE, "ERROR $s")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return emptyArray()
    }

    companion object {
        private val LOG = Logger.getLogger("TrustAllManager")
    }
}

internal object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        var trustManager: X509TrustManager = object : X509TrustManager {
            
            @Throws(CertificateException::class)
            override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
                println("123")
            }

            // does not override
            fun checkServerTrusted(s: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        }
        trustManager = object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                throw CertificateException()
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                checkClientTrusted(x509Certificates, s)
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        }
        trustManager = object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(
                x509Certificates: Array<X509Certificate>,
                s: String,
            ) {
                throw NumberFormatException() // FN, Throws different exception
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
                try {
                    throw CertificateException()
                } catch (e: CertificateException) {
                    e.printStackTrace()
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        }
        var extendedManager: X509ExtendedTrustManager = EmptyX509ExtendedTrustManager()
        
        extendedManager = object : X509ExtendedTrustManager() {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) =
                throw CertificateException()

            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) {
                throw CertificateException()
            }

            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) =
                throw CertificateException()

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, socket: Socket?) {
                throw CertificateException()
            }

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?, engine: SSLEngine?) =
                throw CertificateException()

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                throw CertificateException()
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                TODO("Not yet implemented")
            }

        }
    }
}

internal class EmptyX509ExtendedTrustManager : X509ExtendedTrustManager() {
    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) { // Noncompliant
    } 

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) { // Noncompliant
    } 

    @Throws(CertificateException::class)
    override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
    } 

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) { // Noncompliant
    } 

    @Throws(CertificateException::class)
    override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String, sslEngine: SSLEngine) { // Noncompliant
    } 

    @Throws(CertificateException::class)
    override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
    } 

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return emptyArray()
    }
}

internal interface Coverage {
    fun method()
}
