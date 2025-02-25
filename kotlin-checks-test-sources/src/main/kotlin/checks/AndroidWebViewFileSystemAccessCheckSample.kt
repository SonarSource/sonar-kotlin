package checks

import android.webkit.WebSettings

// region non compliant scenarios

fun setPropertiesToTrue(settings: WebSettings) {
    settings.allowFileAccess = true // Noncompliant {{Make sure exposing the Android file system is safe here.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    settings.allowFileAccessFromFileURLs = true // Noncompliant
    settings.allowUniversalAccessFromFileURLs = true // Noncompliant
    settings.allowContentAccess = true // Noncompliant
}

fun setPropertiesToTrueVal(settings: WebSettings) {
    val trueVal = true
    settings.allowFileAccess = trueVal // Noncompliant
    settings.allowFileAccessFromFileURLs = trueVal // Noncompliant
    settings.allowUniversalAccessFromFileURLs = trueVal // Noncompliant
    settings.allowContentAccess = trueVal // Noncompliant
}

fun setPropertiesToTrueVar(settings: WebSettings) {
    var trueVar = true
    settings.allowFileAccess = trueVar // FN, trueVar may have been changed since previous assignment
    settings.allowFileAccessFromFileURLs = trueVar // FN, trueVar may have been changed since previous assignment
    settings.allowUniversalAccessFromFileURLs = trueVar // FN, trueVar may have been changed since previous assignment
    settings.allowContentAccess = trueVar // FN, trueVar may have been changed since previous assignment
}

fun usingSettersWithTrue(settings: WebSettings) {
    settings.setAllowFileAccess(true) // Noncompliant
    //       ^^^^^^^^^^^^^^^^^^^^^^^^
    settings.setAllowFileAccessFromFileURLs(true) // Noncompliant
    settings.setAllowUniversalAccessFromFileURLs(true) // Noncompliant
    settings.setAllowContentAccess(true) // Noncompliant
}

fun usingApplyWithTrue(settings: WebSettings) {
    settings.apply {
        allowFileAccess = true // Noncompliant
//      ^^^^^^^^^^^^^^^^^^^^^^
        allowFileAccessFromFileURLs = true // Noncompliant
        allowUniversalAccessFromFileURLs = true // Noncompliant
        allowContentAccess = true // Noncompliant
    }
}

fun usingSettersWithTrueViaApply(settings: WebSettings) {
    settings.apply {
        setAllowFileAccess(true) // Noncompliant
    //  ^^^^^^^^^^^^^^^^^^^^^^^^
        setAllowFileAccessFromFileURLs(true) // Noncompliant
        setAllowUniversalAccessFromFileURLs(true) // Noncompliant
        setAllowContentAccess(true) // Noncompliant
    }
}

fun usingWithWithTrue(settings: WebSettings) {
    with(settings) {
        allowFileAccess = true // Noncompliant
//      ^^^^^^^^^^^^^^^^^^^^^^
        allowFileAccessFromFileURLs = true // Noncompliant
        allowUniversalAccessFromFileURLs = true // Noncompliant
        allowContentAccess = true // Noncompliant
    }
}

fun usingLetWithTrue(settings: WebSettings) {
    settings.let {
        it.allowFileAccess = true // Noncompliant
//      ^^^^^^^^^^^^^^^^^^^^^^^^^
        it.allowFileAccessFromFileURLs = true // Noncompliant
        it.allowUniversalAccessFromFileURLs = true // Noncompliant
        it.allowContentAccess = true // Noncompliant
    }
}

fun usingSafeCallToLetWithTrue(settings: WebSettings?) {
    settings?.let { namedIt ->
        namedIt.allowFileAccess = true // Noncompliant
        namedIt.allowFileAccessFromFileURLs = true // Noncompliant
        namedIt.allowUniversalAccessFromFileURLs = true // Noncompliant
        namedIt.allowContentAccess = true // Noncompliant
    }
}

fun assigningFunToAnotherFun(settings: WebSettings) {
    val fun1 = settings::setAllowFileAccess
    fun1(true) // FN, requires data flow analysis
}

fun assigningConstantBooleanExpression(settings: WebSettings) {
    settings.allowFileAccess = true || false // FN, requires resolving complex constant expressions
}

// endregion

// region compliant scenarios

fun getProperties(settings: WebSettings) {
    val allowFileAccess = settings.allowFileAccess // Compliant
    val allowFileAccessFromFileURLs = settings.allowFileAccessFromFileURLs // Compliant
    val allowUniversalAccessFromFileURLs = settings.allowUniversalAccessFromFileURLs // Compliant
    val allowContentAccess = settings.allowContentAccess // Compliant
}

fun toggleProperties(settings: WebSettings) {
    settings.allowFileAccess = !settings.allowFileAccess // Compliant
    settings.allowFileAccessFromFileURLs = !settings.allowFileAccessFromFileURLs // Compliant
    settings.allowUniversalAccessFromFileURLs = !settings.allowUniversalAccessFromFileURLs // Compliant
    settings.allowContentAccess = !settings.allowContentAccess // Compliant
}

fun setPropertiesToFalse(settings: WebSettings) {
    settings.allowFileAccess = false // Compliant
    settings.allowFileAccessFromFileURLs = false // Compliant
    settings.allowUniversalAccessFromFileURLs = false // Compliant
    settings.allowContentAccess = false // Compliant
}

fun setPropertiesToFalseVal(settings: WebSettings) {
    val falseVal = false
    settings.allowFileAccess = falseVal // Compliant, guaranteed to be false
    settings.allowFileAccessFromFileURLs = falseVal // Compliant, guaranteed to be false
    settings.allowUniversalAccessFromFileURLs = falseVal // Compliant, guaranteed to be false
    settings.allowContentAccess = falseVal // Compliant, guaranteed to be false
}

fun setPropertiesToFalseVar(settings: WebSettings) {
    var falseVar = false
    settings.allowFileAccess = falseVar // Compliant, value may still be false
    settings.allowFileAccessFromFileURLs = falseVar // Compliant, value may still be false
    settings.allowUniversalAccessFromFileURLs = falseVar // Compliant, value may still be false
    settings.allowContentAccess = falseVar // Compliant, value may still be false
}

fun setPropertiesToBooleanParam(settings: WebSettings, value: Boolean) {
    settings.allowFileAccess = value // Compliant, value may be false
    settings.allowFileAccessFromFileURLs = value // Compliant, value may be false
    settings.allowUniversalAccessFromFileURLs = value // Compliant, value may be false
    settings.allowContentAccess = value // Compliant, value may be false
}

fun usingSettersWithFalse(settings: WebSettings) {
    settings.setAllowFileAccess(false) // Compliant, value is false
    settings.setAllowFileAccessFromFileURLs(false) // Compliant, value is false
    settings.setAllowUniversalAccessFromFileURLs(false) // Compliant, value is false
    settings.setAllowContentAccess(false) // Compliant, value is false
}

fun notAWebSettingsObject(settings: NotAWebSettings) {
    settings.allowFileAccess = true // Compliant, not a WebSettings object from android.webkit
}

class NotAWebSettings {
    var allowFileAccess: Boolean = false
}

// endregion
