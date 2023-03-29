package checks

import operators.State
import operators.getValue

fun ComposeSample() {

    val y by remember { state() }



    val x by object: State<String> {override val value = ""}
}

inline fun <T> remember(calculation: () -> T): T = TODO()
private fun state() = object : State<String> {
    override val value = "XY"
}