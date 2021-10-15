package checks

import android.webkit.WebSettings
import okhttp3.ConnectionSpec
import okhttp3.ConnectionSpec.Companion.COMPATIBLE_TLS
import okhttp3.ConnectionSpec.Companion.MODERN_TLS
import okhttp3.OkHttpClient
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import org.apache.commons.net.smtp.SMTPClient
import org.apache.commons.net.smtp.SMTPSClient
import org.apache.commons.net.telnet.TelnetClient
import java.util.Arrays


class ClearTextProtocolCheckSample {
    fun `apache commons noncompliant`() {
        val telnet = TelnetClient() // Noncompliant {{Using Telnet is insecure. Use SSH instead.}}
        telnet.connect("127.0.0.1")

        val ftpClient = FTPClient() // Noncompliant {{Using FTP is insecure. Use SFTP, SCP or FTPS instead.}}
        ftpClient.connect("127.0.0.1", 21)

        val smtpClient =
            SMTPClient() // Noncompliant {{Using clear-text SMTP is insecure. Use SMTP over SSL/TLS or SMTP with STARTTLS instead.}}
        smtpClient.connect("127.0.0.1")
    }

    fun `apache commons compliant`() {
        FTPSClient()
        SMTPSClient()
    }

    fun `okHttp noncompliant`() {
        val client = OkHttpClient.Builder()
            .connectionSpecs(
                listOf(
                    ConnectionSpec.MODERN_TLS,
                    ConnectionSpec.CLEARTEXT // Noncompliant {{Using HTTP is insecure. Use HTTPS instead.}}
                )
            )
            .build()

        val client2 = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.CLEARTEXT)) // Noncompliant {{Using HTTP is insecure. Use HTTPS instead.}} [[sc=37;ec=61]]
            .build();

        val spec =
            ConnectionSpec.Builder(ConnectionSpec.CLEARTEXT) // Noncompliant {{Using HTTP is insecure. Use HTTPS instead.}}
                .build()

        val client3: OkHttpClient = OkHttpClient.Builder()
            .connectionSpecs(listOf(spec))
            .build()


    }

    fun `okHttp compliant`() {
        val client1: OkHttpClient = OkHttpClient.Builder()
            .connectionSpecs(Arrays.asList(MODERN_TLS, COMPATIBLE_TLS)) // Compliant
            .build()

        val client2: OkHttpClient = OkHttpClient.Builder()
            .connectionSpecs(listOf(MODERN_TLS)) // Compliant
            .build()

        val spec: ConnectionSpec = ConnectionSpec.Builder(MODERN_TLS) // Compliant
            .build()

        val client3: OkHttpClient = OkHttpClient.Builder()
            .connectionSpecs(listOf(spec))
            .build()
    }

    fun `android WebSettings noncompliant`(settings: WebSettings, value: Int) {
        settings.mixedContentMode =
            WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // Noncompliant {{Using a relaxed mixed content policy is security-sensitive.}}
        //  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        settings.mixedContentMode = (0) // Noncompliant
        //                           ^

        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW) // Noncompliant {{Using a relaxed mixed content policy is security-sensitive.}}
        //                           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        settings.setMixedContentMode((0)) // Noncompliant
        //                            ^
    }

    fun `android WebSettings compliant`(settings: WebSettings, value: Int) {
        settings.mixedContentMode = value // Compliant
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW // Compliant
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE // Compliant

        settings.setMixedContentMode(value) // Compliant
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW) // Compliant
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE) // Compliant

        // coverage
        settings.mixedContentMode += value // Compliant
        var x = 1
        x = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }

}
