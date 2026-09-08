package checks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Regression test extracted from user false-positive feedback.
fun paginate(repository: Repository) {
    var afterId: String? = null
    while (true) {
        val page = when (val result = repository.loadPage(afterId)) {
            is PageResult.Success -> result.data
            else -> return
        }
        if (page.done) break
        afterId = page.next // Compliant, used to be a false positive with partial semantics.
    }
}

// Regression test extracted from user false-positive feedback.
fun resolve(
    instruments: List<Instrument>,
    mode: Mode,
): Pair<Instrument?, BankError?> {
    val activeInstrument = if (mode == Mode.ACTIVE) instruments.firstOrNull { it.isActive() } else null
    val restoreInstrument = instruments.firstOrNull()

    var matchedBankError: BankError? = null
    val resolvedInstrument = listOfNotNull(activeInstrument, restoreInstrument).firstOrNull {
        val valid = isInstrumentValid(it)
        if (!valid && matchedBankError == null) {
            matchedBankError = latestErrorFor(it) // Compliant, used to be a false positive with partial semantics.
        }
        valid
    }

    if (resolvedInstrument != null) return resolvedInstrument to null
    return null to matchedBankError
}

// Regression test extracted from user false-positive feedback.
fun load(scope: CoroutineScope, host: Host) {
    scope.launch {
        val result = if (host.hasCachedDetails) null else runCatching { host.fetchDetails() }
        var fetchedDetails: Any? = null
        result?.fold(
            onSuccess = {
                fetchedDetails = it // Compliant, used to be a false positive with partial semantics.
            },
            onFailure = { return@launch },
        )
        println(fetchedDetails)
    }
}

fun validAssignmentFinding() {
    var value = 0
    println(value)
    value = 1 // Noncompliant {{The value assigned here is never used.}}
}

fun validCallbackFinding(register: ((Int) -> Unit) -> Unit) {
    var width = 0
    println(width)
    register { width = it } // Noncompliant {{The value assigned here is never used.}}
}

fun otherDiagnosticsRemainEnabled() {
    unresolved()
    var overwritten = 0 // Noncompliant {{Remove this useless initializer.}}
    overwritten = 1
    println(overwritten)

    var neverRead = 0 // Noncompliant {{Remove this variable, which is assigned but never accessed.}}
    neverRead = 1
}

fun outerFindingSurvivesErrorInLocalFunction() {
    fun localFunctionWithError() {
        unresolvedNestedCall()
    }

    var value = 0
    println(value)
    value = 1 // Noncompliant {{The value assigned here is never used.}}
}
