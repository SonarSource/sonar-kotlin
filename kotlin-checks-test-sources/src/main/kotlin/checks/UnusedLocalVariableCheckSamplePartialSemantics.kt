package checks

import okhttp3.RequestBody
import otherpackage.get
import java.io.File

class UnusedLocalVariableCheckSamplePartialSemantics {

    var activity: RequestBody = RequestBody.create(null, File(""))

    fun someMethodThatRequiresActivity() {
        val foo = activity ?: return
        foo.get(1)
    }
}

/**
 * Reproduces the K2 false positive observed under partial semantics: a function-valued local has an
 * error-typed initializer, then its later implicit invocation is not counted as a read.
 * Plain-valued locals did not reproduce the issue.
 */
class FunctionValuedLocalWithUnresolvedInitializer {

    fun functionValuedLocal(body: RequestBody) {
        val contentLength = RequestBody::contentLength // Compliant, used to be a false positive with partial semantics.
        contentLength(body)
    }

    fun delegatedFunctionValuedLocal(body: RequestBody) {
        val contentLength by lazy { RequestBody::contentLength } // Compliant, used to be a false positive with partial semantics.
        contentLength(body)
    }

    fun destructuredFunctionValuedLocal(body: RequestBody) {
        val (contentLength) = Pair(RequestBody::contentLength, Unit) // Compliant, used to be a false positive with partial semantics.
        contentLength(body)
    }

    fun plainValuedLocal(body: RequestBody) {
        // Despite having an unresolved initializer, the later read of `plainValue` is still recognized by K2.
        val plainValue = body.contentLength() // Compliant
        plainValue.inc()
    }

    fun unusedPlainValueLocalWithUnresolvedInitializer(body: RequestBody) {
        // Accepted FN: the workaround suppresses unused locals whose initializer has an error type.
        val unusedValue = body.contentLength()
    }

    fun genuinelyUnusedVariableNextToUnresolvedCall(body: RequestBody) {
        val unusedFunction = Int::inc // Noncompliant {{Remove this unused "unusedFunction" local variable.}}
//          ^^^^^^^^^^^^^^
        body.contentLength()
    }
}
