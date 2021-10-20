package checks

import android.webkit.WebSettings

private const val MY_TRUE = true

class WebViewsFileAccessCheckSample {
    fun foo(settings: WebSettings, value: Boolean) {
        settings.allowFileAccess = true // Noncompliant {{Make sure that enabling file access is safe here.}}
        //                         ^^^^
        settings.setAllowFileAccess(MY_TRUE) // Noncompliant {{Make sure that enabling file access is safe here.}}
        //                          ^^^^^^^

        settings.allowFileAccess = false // Compliant
        settings.setAllowFileAccess(false) // Compliant
        settings.allowFileAccess = value // Compliant
        settings.setAllowFileAccess(value) // Compliant

        settings.allowContentAccess = true // Noncompliant {{Make sure that enabling file access is safe here.}}
        //                            ^^^^
        settings.setAllowContentAccess(true) // Noncompliant {{Make sure that enabling file access is safe here.}}
        //                             ^^^^

        settings.allowContentAccess = false // Compliant
        settings.setAllowContentAccess(false) // Compliant
        settings.allowContentAccess = value // Compliant
        settings.setAllowContentAccess(value) // Compliant

        settings.allowFileAccessFromFileURLs = true // Noncompliant {{Make sure that enabling file access is safe here.}}
        //                                     ^^^^
        settings.setAllowFileAccessFromFileURLs(true) // Noncompliant {{Make sure that enabling file access is safe here.}}
        //                                      ^^^^

        settings.allowFileAccessFromFileURLs = false // Compliant
        settings.setAllowFileAccessFromFileURLs(false) // Compliant
        settings.allowFileAccessFromFileURLs = value // Compliant
        settings.setAllowFileAccessFromFileURLs(value) // Compliant

        settings.allowUniversalAccessFromFileURLs = true // Noncompliant {{Make sure that enabling file access is safe here.}}
        //                                          ^^^^
        settings.setAllowUniversalAccessFromFileURLs(true) // Noncompliant {{Make sure that enabling file access is safe here.}}
        //                                           ^^^^

        settings.allowUniversalAccessFromFileURLs = false // Compliant
        settings.setAllowUniversalAccessFromFileURLs(false) // Compliant
        settings.allowUniversalAccessFromFileURLs = value // Compliant
        settings.setAllowUniversalAccessFromFileURLs(value) // Compliant
    }

    fun coverage(a: Boolean) {
        var x = false
        if (a != true) {
            x = true
        }
    }
}
