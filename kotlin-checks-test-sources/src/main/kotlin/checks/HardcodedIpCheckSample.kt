package checks

class HardcodedIpCheckSample {

    fun f() {
        var x = 120;
        "120";
        var ip = "1.2.3.4"; // Noncompliant {{Make sure using this hardcoded IP address is safe here.}}
//               ^^^^^^^^^
        "1.2.3.4"; // Noncompliant
        "1.2.3.4:80"; // Noncompliant
        "1.2.3.4:8080"; // Noncompliant
        "1.2.3.4:a";
        "1.2.3.4.5";

        // String interpolation:
        "$ip 1.2.3.4";

// Noncompliant@+1 {{Make sure using this hardcoded IP address is safe here.}}
        var url = "http://192.168.0.1/admin.html";
// Noncompliant@+1
        url = "http://192.168.0.1:8181/admin.html";
        var url2 = "http://www.example.org";

        var notAnIp1 = "0.0.0.1234";
        var notAnIp2 = "1234.0.0.0";
        var notAnIp3 = "1234.0.0.0.0.1234";
        var notAnIp4 = ".0.0.0.0";
        var notAnIp5 = "0.256.0.0";

        ip = "0.00.0.0"; // Compliant
        ip = "1.2.03.4"; // Compliant

        var fileName = "v0.0.1.200__do_something.sql"; // Compliant - suffixed and prefixed
        var version = "1.0.0.0-1"; // Compliant - suffixed

        "1080:0:0:0:8:800:200C:417A"; // Noncompliant {{Make sure using this hardcoded IP address is safe here.}}
        "[1080::8:800:200C:417A]"; // Noncompliant
        "::800:200C:417A"; // Noncompliant
        "1080:800:200C::"; // Noncompliant
        "::FFFF:129.144.52.38"; // Noncompliant
        "::129.144.52.38"; // Noncompliant
        "::FFFF:38"; // Noncompliant
        "::100"; // Noncompliant
        "1080:0:0:0:8:200C:131.107.129.8"; // Noncompliant
        "1080:0:0::8:200C:131.107.129.8"; // Noncompliant

        "1080:0:0:0:8:800:200C:417G"; // Compliant - not valid IPv6
        "1080:0:0:0:8::800:200C:417A"; // Compliant - not valid IPv6
        "1080:0:0:0:8:::200C:417A"; // Compliant - not valid IPv6
        "1080:0:0:0:8"; // Compliant - not valid IPv6
        "1080:0::0:0:8::200C:417A"; // Compliant - not valid IPv6
        "1080:0:0:0:8::200C:417A:"; // Compliant - not valid IPv6
        "1080:0:0:0:8::200C:131.107.129.8"; // Compliant - not valid IPv6
        "1080:0:0:0:8::200C:256.256.129.8"; // Compliant - not valid IPv6
        "1080:0:0:0:8:200C:200C:131.107.129.8"; // Compliant - not valid IPv6
        "1080:0:0:0:8:131.107.129.8"; // Compliant - not valid IPv6
        "1080:0::0::8:200C:131.107.129.8"; // Compliant - not valid IPv6
        "1080:0:0:0:8:200C:131.107.129"; // Compliant - not valid IPv6
        "1080:0:0:0:8:200C:417A:131.107"; // Compliant - not valid IPv6

// Noncompliant@+1 {{Make sure using this hardcoded IP address is safe here.}}
        "http://[2001:db8:1f70::999:de8:7648:6e8]";
// Noncompliant@+1
        "http://[2001:db8:1f70::999:de8:7648:6e8]:100/";
// Noncompliant@+1
        "https://[3FFE:1A05:510:1111:0:5EFE:131.107.129.8]:8080/";
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

// Noncompliant@+1
        "https://[3FFE::1111:0:5EFE:131.107.129.8]:8080/";

        "ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"; // Noncompliant

// Exceptions
        "0.0.0.0";
        "::1";
        "000:00::1";
        "255.255.255.255";
        "255.255.255.255:80";
        "2.5.255.255";
        "127.5.255.255";
        "http://[::0]:100/";
        "0000:0000:0000:0000:0000:0000:0000:0000";

    }

}
