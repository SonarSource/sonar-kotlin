package checks

import android.webkit.WebView
import android.webkit.WebViewFactoryProvider.Statics

private const val MY_TRUE = true

class DebugFeatureEnabledCheckSample {
    private fun androidWebView(statics: Statics) {

        WebView.setWebContentsDebuggingEnabled(true) // Noncompliant {{Make sure this debug feature is deactivated before delivering the code in production.}}
        //      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        WebView.setWebContentsDebuggingEnabled(MY_TRUE) // Noncompliant
        WebView.setWebContentsDebuggingEnabled(false)
        statics.setWebContentsDebuggingEnabled(true) // Noncompliant
        //      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        statics.setWebContentsDebuggingEnabled(false)
    }
}
