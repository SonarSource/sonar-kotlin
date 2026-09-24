package checks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// Regression test extracted from user false-positive feedback.
fun processBatches(source: DataSource) {
    var cursor: String? = null
    while (true) {
        val batch = when (val response = source.fetchBatch(cursor)) {
            is Response.Success -> response.value
            else -> return
        }
        if (batch.complete) break
        cursor = batch.next // Compliant, used to be a false positive with partial semantics.
    }
}

// Regression test extracted from user false-positive feedback.
fun selectItem(
    items: List<Item>,
    strategy: Strategy,
): Pair<Item?, Problem?> {
    val preferredItem = if (strategy == Strategy.PREFERRED) items.firstOrNull { it.isPreferred() } else null
    val fallbackItem = items.firstOrNull()

    var problem: Problem? = null
    val selectedItem = listOfNotNull(preferredItem, fallbackItem).firstOrNull {
        val acceptable = isAcceptable(it)
        if (!acceptable && problem == null) {
            problem = problemFor(it) // Compliant, used to be a false positive with partial semantics.
        }
        acceptable
    }

    if (selectedItem != null) return selectedItem to null
    return null to problem
}

// Regression test extracted from user false-positive feedback.
fun fetchValue(scope: CoroutineScope, source: ValueSource) {
    scope.launch {
        val result = if (source.hasCachedValue) null else runCatching { source.fetchValue() }
        var fetchedValue: Any? = null
        result?.fold(
            onSuccess = {
                fetchedValue = it // Compliant, used to be a false positive with partial semantics.
            },
            onFailure = { return@launch },
        )
        println(fetchedValue)
    }
}

val propertyInitializer = run initializer@{
    val result = if (unresolvedCondition()) null else runCatching { unresolvedValue() }
    var assigned: Any? = null
    result?.fold(
        onSuccess = { assigned = it }, // Compliant, used to be a false positive with partial semantics.
        onFailure = { return@initializer null },
    )
    assigned
}

class ClassWithInitializer {
    private var observed: Any? = null

    init {
        run block@{
            val result = if (unresolvedCondition()) null else runCatching { unresolvedValue() }
            var nestedAssigned: Any? = null
            result?.fold(
                onSuccess = { nestedAssigned = it }, // Compliant, used to be a false positive with partial semantics.
                onFailure = { return@block },
            )
            observed = nestedAssigned
        }
    }
}

fun mixedResultsInErrorRegion() {
    unresolved()
    var overwritten = 0 // Noncompliant {{Remove this useless initializer.}}
    overwritten = 1
    println(overwritten)

    var neverRead = 0 // Noncompliant {{Remove this variable, which is assigned but never accessed.}}
    // FN: The assigned value is never read, but the finding is suppressed because `unresolved()`
    // makes this function an error region.
    neverRead = 1
}

val validPropertyFinding = run {
    var value = 0
    println(value)
    value = 1 // Noncompliant {{The value assigned here is never used.}}
    null
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

fun outerFindingSurvivesErrorInLocalFunction() {
    fun localFunctionWithError() {
        unresolvedNestedCall()
    }

    var value = 0
    println(value)
    value = 1 // Noncompliant {{The value assigned here is never used.}}
}
