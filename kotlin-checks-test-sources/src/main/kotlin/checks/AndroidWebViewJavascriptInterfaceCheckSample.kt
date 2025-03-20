package checks

import android.annotation.JavascriptInterface
import android.webkit.WebView

class AndroidWebViewJavascriptInterfaceCheck {
    private val valWebViewProperty: WebView = WebView()
    private var varWebViewProperty: WebView = WebView()

    class JsObject {
        @JavascriptInterface
        override fun toString(): String {
            return "injectedObject"
        }
    }

    class NotAWebView {
        fun addJavascriptInterface(obj: Any?, name: String?) {
        }
    }

    open class WebViewChild : WebView() {
    }

    class WebViewGrandChild : WebViewChild() {
        override fun addJavascriptInterface(obj: Any?, name: String?) {
        }
    }

    fun nonCompliantScenarios(webViewParam: WebView) {
        webViewParam.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant {{Exposing a Javascript interface can expose sensitive information to attackers. Make sure it is safe here.}}
//                   ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        WebView().addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
        valWebViewProperty.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
        varWebViewProperty.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
        val valWebViewLocal = WebView()
        valWebViewLocal.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
        var varWebViewLocal = WebView()
        varWebViewLocal.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant

        val valJsObjectLocal = JsObject()
        webViewParam.addJavascriptInterface(valJsObjectLocal, "injectedObject") // Noncompliant
        val valInjectedName = "injectedObject"
        webViewParam.addJavascriptInterface(JsObject(), valInjectedName) // Noncompliant
        webViewParam.addJavascriptInterface(valJsObjectLocal, valInjectedName) // Noncompliant

        val valWebViewClosure = WebView()
        val valInjectedNameClosure = "injectedObject"
        val valJsObjectLocalClosure = JsObject()
        fun closure() {
            valWebViewClosure.addJavascriptInterface(valJsObjectLocalClosure, valInjectedNameClosure) // Noncompliant
        }

        // Complex expressions as parameters
        webViewParam.addJavascriptInterface(if (true) JsObject() else JsObject(), "injectedObject") // Noncompliant
        webViewParam.addJavascriptInterface(valJsObjectLocal, valInjectedName.toString()) // Noncompliant
        webViewParam.addJavascriptInterface(JsObject(), valInjectedName + "suffix") // Noncompliant
        webViewParam.let {
            it.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
        }
        webViewParam.let { theWebView ->
            theWebView.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
        }
        webViewParam?.let {
            it.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
            it?.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
        }
        webViewParam.run {
            addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
        }
        webViewParam.apply {
            addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
            this.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
        }
        with(webViewParam) {
            addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
            this.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
        }

        val derivedFromWebView = WebViewChild()
        derivedFromWebView.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant

        val derivedFromWebViewBaseType: WebView = WebViewChild()
        derivedFromWebViewBaseType.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant

        val derivedFromWebViewGrandChild = WebViewGrandChild()
        derivedFromWebViewGrandChild.addJavascriptInterface(JsObject(), "injectedObject") // Noncompliant
    }

    fun compliantScenarios(webView: WebView, notAWebView: NotAWebView) {
        WebView() // Compliant, no method invoked on the WebView
        WebView().hashCode() // Compliant, different method invoked on the WebView
        notAWebView.addJavascriptInterface(JsObject(), "injectedObject") // Compliant, not a WebView
        notAWebView.let {
            it.addJavascriptInterface(JsObject(), "injectedObject") // Compliant, not a WebView
            it?.addJavascriptInterface(JsObject(), "injectedObject") // Compliant, not a WebView
        }
        with(notAWebView) {
            addJavascriptInterface(JsObject(), "injectedObject") // Compliant, not a WebView
            this.addJavascriptInterface(JsObject(), "injectedObject") // Compliant, not a WebView
        }
    }
}
