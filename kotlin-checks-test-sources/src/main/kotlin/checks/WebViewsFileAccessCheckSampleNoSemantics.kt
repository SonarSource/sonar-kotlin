package checks

import android.webkit.WebSettings

class WebViewsFileAccessCheckSampleNoSemantics {

    // region non compliant scenarios

    fun noSemantics_setPropertiesToTrue(settings: WebSettings) {
        settings.allowFileAccess = true // FN
        settings.allowFileAccessFromFileURLs = true // FN
        settings.allowUniversalAccessFromFileURLs = true // FN
        settings.allowContentAccess = true // FN
    }

    fun noSemantics_usingApplyWithTrue(settings: WebSettings) {
        settings.apply {
            allowFileAccess = true // FN
            allowFileAccessFromFileURLs = true // FN
            allowUniversalAccessFromFileURLs = true // FN
            allowContentAccess = true // FN
        }
    }

    fun noSemantics_usingWithWithTrue(settings: WebSettings) {
        with(settings) {
            allowFileAccess = true // FN
            allowFileAccessFromFileURLs = true // FN
            allowUniversalAccessFromFileURLs = true // FN
            allowContentAccess = true // FN
        }
    }

    fun noSemantics_usingLetWithTrue(settings: WebSettings) {
        settings.let {
            it.allowFileAccess = true // FN
            it.allowFileAccessFromFileURLs = true // FN
            it.allowUniversalAccessFromFileURLs = true // FN
            it.allowContentAccess = true // FN
        }
    }

    // endregion

    // region compliant scenarios

    fun noSemantics_getProperties(settings: WebSettings) {
        val allowFileAccess = settings.allowFileAccess // Compliant
        val allowFileAccessFromFileURLs = settings.allowFileAccessFromFileURLs // Compliant
        val allowUniversalAccessFromFileURLs = settings.allowUniversalAccessFromFileURLs // Compliant
        val allowContentAccess = settings.allowContentAccess // Compliant
    }

    fun noSemantics_toggleProperties(settings: WebSettings) {
        settings.allowFileAccess = !settings.allowFileAccess // Compliant
        settings.allowFileAccessFromFileURLs = !settings.allowFileAccessFromFileURLs // Compliant
        settings.allowUniversalAccessFromFileURLs = !settings.allowUniversalAccessFromFileURLs // Compliant
        settings.allowContentAccess = !settings.allowContentAccess // Compliant
    }

    fun noSemantics_setPropertiesToFalse(settings: WebSettings) {
        settings.allowFileAccess = false // Compliant
        settings.allowFileAccessFromFileURLs = false // Compliant
        settings.allowUniversalAccessFromFileURLs = false // Compliant
        settings.allowContentAccess = false // Compliant
    }

    fun noSemantics_setPropertiesToFalseVal(settings: WebSettings) {
        val falseVal = false
        settings.allowFileAccess = falseVal // Compliant, guaranteed to be false
        settings.allowFileAccessFromFileURLs = falseVal // Compliant, guaranteed to be false
        settings.allowUniversalAccessFromFileURLs = falseVal // Compliant, guaranteed to be false
        settings.allowContentAccess = falseVal // Compliant, guaranteed to be false
    }

    fun noSemantics_setPropertiesToFalseVar(settings: WebSettings) {
        var falseVar = false
        settings.allowFileAccess = falseVar // Compliant, value may still be false
        settings.allowFileAccessFromFileURLs = falseVar // Compliant, value may still be false
        settings.allowUniversalAccessFromFileURLs = falseVar // Compliant, value may still be false
        settings.allowContentAccess = falseVar // Compliant, value may still be false
    }

    fun noSemantics_setPropertiesToBooleanParam(settings: WebSettings, value: Boolean) {
        settings.allowFileAccess = value // Compliant, value may be false
        settings.allowFileAccessFromFileURLs = value // Compliant, value may be false
        settings.allowUniversalAccessFromFileURLs = value // Compliant, value may be false
        settings.allowContentAccess = value // Compliant, value may be false
    }

    fun noSemantics_usingSettersWithFalse(settings: WebSettings) {
        settings.setAllowFileAccess(false) // Compliant, value is false
        settings.setAllowFileAccessFromFileURLs(false) // Compliant, value is false
        settings.setAllowUniversalAccessFromFileURLs(false) // Compliant, value is false
        settings.setAllowContentAccess(false) // Compliant, value is false
    }

    fun noSemantics_notAWebSettingsObject(settings: NoSemantics_NotAWebSettings) {
        settings.allowFileAccess = true // Compliant, not a WebSettings object from android.webkit
    }

    class NoSemantics_NotAWebSettings {
        var allowFileAccess: Boolean = false
    }

    // endregion

}
