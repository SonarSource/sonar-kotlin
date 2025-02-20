package checks

import android.net.http.SslCertificate
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.Socket
import java.security.cert.Certificate
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

// region Java Cryptography Extension scenarios

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

internal class TrustAllManagerNullable : X509TrustManager {
    @Throws(CertificateException::class)
    override fun checkClientTrusted( // Noncompliant {{Enable server certificate validation on this SSL/TLS connection.}}
        x509Certificates: Array<X509Certificate?>,
        s: String,
    ) {
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(x509Certificates: Array<X509Certificate?>, s: String) { // Noncompliant
        LOG.log(Level.SEVERE, "ERROR $s")
    }

    override fun getAcceptedIssuers(): Array<X509Certificate?> {
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

            fun checkServerTrusted(s: String?) {} // Compliant - function signature does not belong to X59TrustManager

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        }
        trustManager = object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                if (true)
                    throw CertificateException()
                else
                    throw RuntimeException()
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                checkClientTrusted(x509Certificates, s)
                getAcceptedIssuers()
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        }
        trustManager = object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) {  // Noncompliant
                throw NumberFormatException()
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) { // Noncompliant
                try {
                    throw CertificateException()
                } catch (e: CertificateException) {
                    e.printStackTrace()
                } catch (e: RuntimeException) {
                    e.printStackTrace()
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        }
        trustManager = object : X509TrustManager {
            @Throws(CertificateException::class)
            override fun checkClientTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                try {
                    getAcceptedIssuers()
                } catch (e: RuntimeException) {
                    throw CertificateException()
                }
            }

            @Throws(CertificateException::class)
            override fun checkServerTrusted(x509Certificates: Array<X509Certificate>, s: String) {
                try {
                    getAcceptedIssuers()
                } catch (e: RuntimeException) {
                    try {
                        getAcceptedIssuers()
                    } catch (e: IllegalAccessException) {
                        throw CertificateException()
                    }
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

// endregion

// region Android WebView non compliant scenarios

class WebViewClientAlwaysProceedingWithBody : WebViewClient() {
    override fun onReceivedSslError( // Noncompliant {{Enable server certificate validation on this SSL/TLS connection.}}
        //       ^^^^^^^^^^^^^^^^^^
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        handler.proceed()
    }
}

class WebViewClientAlwaysProceedingInline : WebViewClient() {
    override fun onReceivedSslError( // Noncompliant
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) = handler.proceed()
}

class WebViewClientAlwaysProceedingWithComment : WebViewClient() {
    override fun onReceivedSslError( // Noncompliant
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        // This is a temporary workaround for development, remove in production
        handler.proceed()
    }
}

class WebViewClientLoggingAndAlwaysProceeding : WebViewClient() {
    override fun onReceivedSslError( // Noncompliant
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        LOG.log(Level.SEVERE, "ERROR $error")
        handler.proceed()
    }

    companion object {
        private val LOG = Logger.getLogger("WebViewClientLoggingAndAlwaysProceeding")
    }
}

class WebViewClientUnreachableCancelThenProceed : WebViewClient() {
    override fun onReceivedSslError( // FN, would require SE to determine that it always proceeds
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        if (false) {
            handler.cancel()
        }
        handler.proceed()
    }
}

class WebViewClientUnreachableCancelInShortCircuitThenProceed : WebViewClient() {
    override fun onReceivedSslError( // FN, would require SE to determine that it always proceeds
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        false && (view.hashCode() == 42).also { handler.cancel() }
        handler.proceed()
    }
}

class WebViewClientAlwaysProceedingWithSafeCall : WebViewClient() {
    override fun onReceivedSslError( // FN, no easy way to distinguish from a conditional proceed
        view: WebView,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        handler?.proceed()
    }
}

class WebViewClientAlwaysProceedingWithElvisOperator : WebViewClient() {
    override fun onReceivedSslError( // FN, no easy way to distinguish from a conditional proceed
        view: WebView,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        handler ?: return
        handler.proceed()
    }
}

class WebViewClientAlwaysProceedingWithGuardClause : WebViewClient() {
    override fun onReceivedSslError( // FN, no easy way to distinguish from a conditional proceed
        view: WebView,
        handler: SslErrorHandler?,
        error: SslError?
    ) {
        if (handler == null) return
        handler.proceed()
    }
}

class WebViewClientProceedingInLetOnNullableHandler : WebViewClient() {
    override fun onReceivedSslError( // FN, no easy way to distinguish from a conditional proceed
        view: WebView,
        handler: SslErrorHandler?,
        error: SslError
    ) {
        handler?.let { it.proceed() }
    }
}

// endregion

// region Android WebView compliant scenarios

private fun SslCertificate.isServerCertificateValid(): Boolean = true

open class CustomWebViewClient {
    open fun onReceivedSslError( // Compliant, not a WebViewClient derivation
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        handler.cancel()
    }
}

class WebViewClientCallingSuper : WebViewClient() {
    override fun onReceivedSslError( // Compliant, calls default behavior, which is to cancel
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        super.onReceivedSslError(view, handler, error)
    }
}

class WebViewClientCallingSuperAlsoLogging : WebViewClient() {
    override fun onReceivedSslError( // Compliant, calls default behavior, which is to cancel
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) = super.onReceivedSslError(view, handler, error).also {
        LOG.log(Level.SEVERE, "ERROR $error")
    }

    companion object {
        private val LOG = Logger.getLogger("WebViewClientCallingSuperAlsoLogging")
    }
}

class WebViewClientProceedingOrCallingSuper : WebViewClient() {
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

class WebViewClientLoggingThenCallingSuper : WebViewClient() {
    override fun onReceivedSslError( // Compliant, calls default behavior, which is to cancel
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        LOG.log(Level.SEVERE, "ERROR $error")
        super.onReceivedSslError(view, handler, error)
    }

    companion object {
        private val LOG = Logger.getLogger("WebViewClientLoggingThenCallingSuper")
    }
}

class NonWebViewClientAlwaysProceedingWithBody : CustomWebViewClient() {
    override fun onReceivedSslError( // Compliant: not a WebViewClient
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        handler.proceed()
    }
}

class WebViewClientConditionallyProceeding : WebViewClient() {
    override fun onReceivedSslError( // Compliant: default is to cancel
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        if (error.certificate.isServerCertificateValid()) {
            handler.proceed()
        }
    }
}

class WebViewClientConditionallyProceedingInTryCatch : WebViewClient() {
    override fun onReceivedSslError( // Compliant
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        try {
            if (error.certificate.isServerCertificateValid()) {
                handler.proceed()
            }
        } catch (e: Exception) {
            handler.cancel()
        }
    }

    companion object {
        private val LOG = Logger.getLogger("WebViewClientLoggingAndAlwaysProceeding")
    }
}

class WebViewClientConditionallyProceedingInTryWithCatchNotCancelling : WebViewClient() {
    override fun onReceivedSslError( // Compliant
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        try {
            if (error.certificate.isServerCertificateValid()) {
                handler.proceed()
            }
        } catch (e: Exception) {
            // Default is to cancel
            LOG.log(Level.SEVERE, "ERROR $error: certificate could not be validated")
        }
    }

    companion object {
        private val LOG = Logger.getLogger("WebViewClientLoggingAndAlwaysProceeding")
    }
}

class WebViewClientConditionallyProceedingInLoop : WebViewClient() {
    override fun onReceivedSslError( // Compliant
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        while (error.certificate.isServerCertificateValid()) {
            handler.proceed()
            break
        }
    }
}

class WebViewClientAlwaysCancelling : WebViewClient() {
    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        handler.cancel()
    }
}

class WebViewClientConditionallyProceedingAndCancelling : WebViewClient() {
    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        if (error.certificate.isServerCertificateValid()) {
            handler.proceed()
        } else {
            handler.cancel()
        }
    }
}

class WebViewClientConditionallyProceedingAndCancellingWithWhen : WebViewClient() {
    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        when {
            error.certificate.isServerCertificateValid() -> handler.proceed()
            else -> handler.cancel()
        }
    }
}

class WebViewClientConditionallyProceedingAndCancellingWithLetAndWhen : WebViewClient() {
    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) = error.certificate.let {
            when {
                it.isServerCertificateValid() -> handler.proceed()
                else -> handler.cancel()
            }
        }
}

class WebViewClientConditionallyProceedingAndCancellingWithElvis : WebViewClient() {
    override fun onReceivedSslError(
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        val validCertificate = error.certificateIfValidOrNull() ?: return
        handler.proceed()
    }

    private fun SslError?.certificateIfValidOrNull(): android.net.http.SslCertificate? =
        this?.certificate?.takeIf { it.isServerCertificateValid() }
}

class WebViewClientDelegatingChoiceToDialog : WebViewClient() {
    override fun onReceivedSslError( // Compliant: choice is delegated to dialog
        view: WebView,
        handler: SslErrorHandler,
        error: SslError
    ) {
        val dialog = CustomDialog(view, handler, error)
        dialog.show()
    }

    class CustomDialog(
        private val view: WebView,
        private val handler: SslErrorHandler,
        private val error: SslError
    ) {
        fun show() {
            if (error.certificate.isServerCertificateValid()) {
                handler.proceed()
            } else {
                handler.cancel()
            }
        }
    }
}

// endregion
