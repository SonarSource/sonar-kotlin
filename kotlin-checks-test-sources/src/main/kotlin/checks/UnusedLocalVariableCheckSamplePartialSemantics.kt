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
