package checks

import android.net.http.SslCertificate
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.Socket
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

// region Java Cryptography Extension scenarios

class ServerCertificateCheckSampleNoSemantics {
}

internal class TrustAllManager2 : X509TrustManager {
    @Throws(CertificateException::class)
    override fun checkClientTrusted( // Noncompliant
//               ^^^^^^^^^^^^^^^^^^
        x509Certificates: Array<X509Certificate>,
        s: String,
    ) {
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
//               ^^^^^^^^^^^^^^^^^^
        LOG.log(Level.SEVERE, "ERROR $s")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return emptyArray()
    }

    companion object {
        private val LOG = Logger.getLogger("TrustAllManager")
    }
}

internal object Main2 {
    @JvmStatic
    fun main(args: Array<String>) {
        var trustManager: X509TrustManager = object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
//                       ^^^^^^^^^^^^^^^^^^
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
//                       ^^^^^^^^^^^^^^^^^^
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
            override fun checkClientTrusted( // Noncompliant
//                       ^^^^^^^^^^^^^^^^^^
                x509Certificates: Array<X509Certificate>,
                s: String,
            ) {
                "error".toInt()
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
//                       ^^^^^^^^^^^^^^^^^^
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

internal class EmptyX509ExtendedTrustManager2 : X509ExtendedTrustManager() {
    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) { // Noncompliant
//               ^^^^^^^^^^^^^^^^^^
    }

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String, engine: SSLEngine) { // Noncompliant
//               ^^^^^^^^^^^^^^^^^^
    }

    @Throws(CertificateException::class)
    override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
//               ^^^^^^^^^^^^^^^^^^
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String, socket: Socket) { // Noncompliant
//               ^^^^^^^^^^^^^^^^^^
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String, sslEngine: SSLEngine) { // Noncompliant
//               ^^^^^^^^^^^^^^^^^^
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
//               ^^^^^^^^^^^^^^^^^^
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> {
        return emptyArray()
    }
}

internal interface Coverage2 {
    fun method()
}

// endregion

// region Android WebView non compliant scenarios

class NoSemantics_WebViewClientAlwaysProceedingWithBody : WebViewClient() {
    override fun onReceivedSslError( // FN, due to lack of semantics
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        handler.proceed()
    }
}

class NoSemantics_WebViewClientWithFqnParamsAlwaysProceedingWithBody : WebViewClient() {
    override fun onReceivedSslError( // FN, due to lack of semantics
        view: android.webkit.WebView,
        handler: android.webkit.SslErrorHandler,
        error: android.net.http.SslError
    ) {
        handler.proceed()
    }
}

// endregion

// region Android WebView compliant scenarios

private fun SslCertificate.isServerCertificateValid(): Boolean = true

open class NoSemantics_CustomWebViewClient {
    open fun onReceivedSslError( // Compliant, not a WebViewClient derivation
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        handler.cancel()
    }
}

class NoSemantics_WebViewClientCallingSuper : WebViewClient() {
    override fun onReceivedSslError( // Compliant, calls default behavior, which is to cancel
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        super.onReceivedSslError(view, handler, error)
    }
}

class NoSemantics_WebViewClientProceedingOrCallingSuper : WebViewClient() {
    override fun onReceivedSslError( // Compliant, calls default behavior, which is to cancel
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        if (error.certificate.isServerCertificateValid()) {
            handler.proceed()
        } else {
            super.onReceivedSslError(view, handler, error)
        }
    }
}

// endregion
