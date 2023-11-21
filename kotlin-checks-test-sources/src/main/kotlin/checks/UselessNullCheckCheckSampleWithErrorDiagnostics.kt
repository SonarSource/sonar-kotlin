package checks

class UselessNullCheckCheckSampleWithErrorDiagnostics {
    fun foo() {
        null!! // Compliant
        null!! // Noncompliant
    }
}
