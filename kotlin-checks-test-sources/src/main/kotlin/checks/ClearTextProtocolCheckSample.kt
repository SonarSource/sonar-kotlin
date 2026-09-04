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
        val telnet = TelnetClient() // Noncompliant {{Using Telnet protocol is insecure. Use SSH instead.}}
        telnet.connect("127.0.0.1")

        val ftpClient = FTPClient() // Noncompliant {{Using FTP protocol is insecure. Use SFTP, SCP or FTPS instead.}}
        ftpClient.connect("127.0.0.1", 21)

        val smtpClient = SMTPClient() // Noncompliant {{Using SMTP protocol is insecure. Use SMTPS instead.}}
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
                    ConnectionSpec.CLEARTEXT // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
                )
            )
            .build()

        val client2 = OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.CLEARTEXT)) // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}} [[sc=37;ec=61]]
            .build();

        val spec =
            ConnectionSpec.Builder(ConnectionSpec.CLEARTEXT) // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
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

    fun `url literals noncompliant`() {
        val api = "http://api.acme.com/v1/users" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        //        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        val files = "ftp://files.acme.com/data" // Noncompliant {{Using FTP protocol is insecure. Use SFTP, SCP or FTPS instead.}}
        val legacy = "telnet://legacy.acme.com" // Noncompliant {{Using Telnet protocol is insecure. Use SSH instead.}}
        val socket = "ws://socket.acme.com/live" // Noncompliant {{Using WS protocol is insecure. Use WSS instead.}}
        val mail = "smtp://mail.acme.com" // Noncompliant {{Using SMTP protocol is insecure. Use SMTPS instead.}}
        val directory = "ldap://ldap.acme.com/dc=acme" // Noncompliant {{Using LDAP protocol is insecure. Use LDAPS instead.}}
        val upperCaseScheme = "HTTP://API.ACME.COM" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val raw = """http://api.acme.com""" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val withCredentials = "http://user:pwd@api.acme.com" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val underscoreHost = "http://my_host.acme.com" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
    }

    fun `url literals compliant - secure schemes`() {
        val secure = "https://api.acme.com/v1/users" // Compliant
        val secureSocket = "wss://socket.acme.com/live" // Compliant
        val secureFiles = "sftp://files.acme.com/data" // Compliant
        val notAUrl = "the api lives at http://api.acme.com" // Compliant - the URL is not the whole literal
    }

    fun `url literals compliant - internal hosts`() {
        val local = "http://localhost:8080/api" // Compliant
        val loopback = "http://127.0.0.1/health" // Compliant
        val loopbackShort = "http://127.1" // Compliant
        val loopbackV6 = "http://[::1]:9000/health" // Compliant
        val awsImds = "http://169.254.169.254/latest/meta-data/" // Compliant
        val azureWireserver = "http://168.63.129.16/machine" // Compliant
        val alibabaImds = "http://100.100.100.200/latest/meta-data/" // Compliant
        val gcpImds = "http://metadata.google.internal/computeMetadata/v1/" // Compliant
        val docker = "http://host.docker.internal:5000" // Compliant
        val kubernetes = "http://payments.default.svc.cluster.local/health" // Compliant
        val singleLabel = "http://payments-service:8080/health" // Compliant
        val androidEmulatorHost = "http://10.0.2.2:8080/api" // Compliant - the emulator's alias for the dev machine
        val androidEmulatorRouter = "http://10.0.2.1" // Compliant
        val androidEmulatorDevice = "http://10.0.2.15/status" // Compliant
        val notTheEmulatorNetwork = "http://10.0.2.7/api" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
    }

    fun `url literals compliant - namespace uri authorities`() {
        val xmlSchema = "http://www.w3.org/2001/XMLSchema-instance" // Compliant
        val android = "http://schemas.android.com/apk/res/android" // Compliant
        val soap = "http://schemas.xmlsoap.org/soap/envelope/" // Compliant
        val spring = "http://www.springframework.org/schema/beans" // Compliant
        val maven = "http://maven.apache.org/POM/4.0.0" // Compliant
        val fhir = "http://hl7.org/fhir/StructureDefinition/Patient" // Compliant
        val schemaOrg = "http://schema.org/Person" // Compliant
    }

    fun `url literals compliant - documentation domains`() {
        val exampleCom = "http://example.com/docs" // Compliant
        val exampleOrgSubdomain = "http://api.example.org/v1" // Compliant
        val exampleNet = "http://example.net" // Compliant
        val exampleTld = "http://acme.example" // Compliant
        val testTld = "http://acme.test/fixture" // Compliant
        val localhostTld = "http://acme.localhost:3000" // Compliant
    }

    fun `url literals compliant - no connection target`(url: String, host: String) {
        val schemePrefix = "http://" // Compliant - a prefix constant, it names no endpoint
        val normalized = url.replace("http://", "https://") // Compliant
        val isPlainHttp = url.startsWith("http://api.acme.com") // Compliant
        val isPlainFtp = url.endsWith("ftp://files.acme.com") // Compliant
        val stripped = url.removePrefix("http://api.acme.com") // Compliant
        val strippedSuffix = url.removeSuffix("http://api.acme.com") // Compliant
        val interpolated = "http://$host/health" // Compliant - the host is unknown
        val escapedPlaceholder = "http://\$HOST:\$EXTERNAL_PORT" // Compliant - the host is substituted at runtime
        val bracedPlaceholder = "http://{host}/health" // Compliant - the host is substituted at runtime
        val printfPlaceholder = "http://%s:8080/health" // Compliant - the host is substituted at runtime
        val indexedPrintfPlaceholder = "http://%1${'$'}s/health" // Compliant - the host is substituted at runtime
        val formattedHost = "http://%s/health".format(host) // Compliant - the host is substituted at runtime
        val formattedViaCompanion = String.format("http://%s/health", host) // Compliant
        // The host is fixed here; only the path is formatted, so it is still a clear-text endpoint
        val formattedPath = "http://api.acme.com/%s".format(host) // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val escapedTab = "http://api.acme.com/a\tb" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
    }

    private fun connect(url: String) = url

    fun `url literals noncompliant - remaining clear-text schemes`() {
        val gopher = "gopher://gopher.acme.com/1" // Noncompliant {{Using Gopher protocol is insecure. Use HTTPS instead.}}
        val tftp = "tftp://boot.acme.com/pxelinux.0" // Noncompliant {{Using TFTP protocol is insecure. Use SFTP instead.}}
        val imap = "imap://mail.acme.com" // Noncompliant {{Using IMAP protocol is insecure. Use IMAPS instead.}}
        val pop3 = "pop3://mail.acme.com" // Noncompliant {{Using POP3 protocol is insecure. Use POP3S instead.}}
        val amqp = "amqp://broker.acme.com" // Noncompliant {{Using AMQP protocol is insecure. Use AMQPS instead.}}
        val mqtt = "mqtt://broker.acme.com" // Noncompliant {{Using MQTT protocol is insecure. Use MQTTS instead.}}
        val sip = "sip://voice.acme.com" // Noncompliant {{Using SIP protocol is insecure. Use SIPS instead.}}
        val rtmp = "rtmp://stream.acme.com/live" // Noncompliant {{Using RTMP protocol is insecure. Use RTMPS instead.}}
        val irc = "irc://chat.acme.com" // Noncompliant {{Using IRC protocol is insecure. Use IRCS instead.}}
        val nntp = "nntp://news.acme.com" // Noncompliant {{Using NNTP protocol is insecure. Use NNTPS instead.}}
        val stomp = "stomp://broker.acme.com" // Noncompliant {{Using STOMP protocol is insecure. Use STOMPS instead.}}
    }

    fun `url literals - scheme matching`() {
        val mixedCase = "HtTp://api.acme.com" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        // "tftp://" must not be mistaken for the "ftp://" prefix it ends with
        val tftpNotFtp = "tftp://boot.acme.com" // Noncompliant {{Using TFTP protocol is insecure. Use SFTP instead.}}
        val unknownScheme = "httpx://api.acme.com" // Compliant - not a clear-text scheme
        val singleSlash = "http:/api.acme.com" // Compliant - not a URL
        val schemeRelative = "//api.acme.com/v1" // Compliant - no scheme
        val opaque = "mailto:someone@acme.com" // Compliant
    }

    fun `url literals - authority is required`() {
        val emptyAuthorityPath = "http:///v1/users" // Compliant - names no host
        val emptyAuthorityQuery = "http://?debug=true" // Compliant - names no host
        val emptyAuthorityFragment = "http://#section" // Compliant - names no host
    }

    fun `url literals - safe-host boundaries are anchored`() {
        // Each host below merely starts with, or contains, a safe host: none of them is one.
        val loopbackPrefix = "http://127.0.0.1.evil.com" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val localhostPrefix = "http://localhost.evil.com" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val kubernetesPrefix = "http://payments.svc.cluster.local.evil.com" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val documentationPrefix = "http://example.com.evil.com" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val notADocumentationDomain = "http://notexample.com" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val documentationLabelInside = "http://example.company.com" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        // test.com is a registrable domain, unlike the RFC 6761 ".test" TLD
        val testDotCom = "http://test.com/crl" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val namespaceAuthorityAsPath = "http://acme.com/www.w3.org/2001/XMLSchema" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
    }

    fun `url literals - numeric hosts`() {
        val hexLoopback = "http://0x7f000001" // Compliant - 127.0.0.1 written in hexadecimal
        val awsIpv6Imds = "http://[fd00:ec2::254]/latest/meta-data/" // Compliant
        val ipv6LoopbackLongForm = "http://[0:0:0:0:0:0:0:1]:9000" // Compliant
        val linkLocal = "http://169.254.1.1/status" // Compliant
        val genericMetadata = "http://metadata.internal/computeMetadata" // Compliant
        val dockerGateway = "http://gateway.docker.internal:2375" // Compliant
        // A dotless numeric host is an IP address, not an unresolvable single-label name
        val decimalIpLiteral = "http://2130706433" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
    }

    fun `url literals - string template entries`() {
        val unicodeEscape = "\u0068ttp://api.acme.com" // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val blockInterpolation = "http://${host()}/health" // Compliant - the host is unknown
        val interpolationAfterHost = "http://api.acme.com/$id" // Compliant FN due to string interpolation
        val paddedRawString = """  http://api.acme.com  """ // Compliant FN - the scheme is not at the start of the literal
    }

    fun `url literals - string pattern APIs`(url: String, path: java.nio.file.Path) {
        val replaced = url.replace("http://api.acme.com", "https://api.acme.com") // Compliant
        val contained = url.contains("http://api.acme.com") // Compliant
        val parts = url.split("http://api.acme.com") // Compliant
        val after = url.substringAfter("http://api.acme.com") // Compliant
        val before = url.substringBefore("http://api.acme.com") // Compliant
        // Same name, different type: this one is a real path comparison, not a string pattern
        val onAPath = path.startsWith("http://api.acme.com") // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val onAList = listOf("a").contains("http://api.acme.com") // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
    }

    fun `url literals - prefix tests`(url: String) {
        val namedArgument = url.startsWith(prefix = "http://api.acme.com") // Compliant
        val extraArgument = url.startsWith("http://api.acme.com", ignoreCase = true) // Compliant
        val nested = require(url.startsWith("http://api.acme.com")) // Compliant
        val redundantParentheses = url.startsWith(("http://api.acme.com")) // Compliant
        val doubleParentheses = url.removePrefix((("http://api.acme.com"))) // Compliant
        // Any other call keeps reporting: the exclusion is about string pattern APIs, not about being an argument
        val connected = connect("http://api.acme.com") // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val listed = listOf("http://api.acme.com") // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val receiver = "http://api.acme.com".length // Noncompliant {{Using HTTP protocol is insecure. Use HTTPS instead.}}
        val startsWithSafeUrl = url.startsWith("http://localhost") // Compliant
    }

    private fun host() = "api.acme.com"

    private val id = 42
}
