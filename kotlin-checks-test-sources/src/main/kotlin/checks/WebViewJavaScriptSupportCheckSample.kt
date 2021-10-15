package checks

import android.webkit.WebSettings

private const val MY_TRUE = true

class WebViewJavaScriptSupportCheckSample {

    fun foo(settings: WebSettings, value: Boolean) {
        settings.javaScriptEnabled = true // Noncompliant {{Make sure that enabling JavaScript support is safe here.}}
        //                           ^^^^
        settings.javaScriptEnabled = MY_TRUE // Noncompliant
        settings.javaScriptEnabled = false // Compliant
        settings.javaScriptEnabled = value // Compliant

        settings.setJavaScriptEnabled(true) // Noncompliant {{Make sure that enabling JavaScript support is safe here.}}
        //                            ^^^^
        settings.setJavaScriptEnabled(false) // Compliant
        settings.setJavaScriptEnabled(value) // Compliant

        // coverage
        if (settings.javaScriptEnabled == value) {
            var x = false
            x = true
        }
    }
}
