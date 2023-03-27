package checks

class SamConversionCheckSample {

    val resource = loadResource(object : ProgressCallback { // Noncompliant {{Replace explicit functional interface implementation with lambda expression.}}
        //                      ^^^^^^^^^^^^^^^^^^^^^^^^^
        override fun progressChanged(percent: Double) {
            // ...
        }
    })

    val callback = object : ProgressCallback { // Noncompliant {{Replace explicit functional interface implementation with lambda expression.}}
        //         ^^^^^^^^^^^^^^^^^^^^^^^^^
        override fun progressChanged(percent: Double) {
            // ...
        }
    }

    val twoInterfacesImplemented =
        loadResource(object : ProgressCallback, Comparator<Int> { // Compliant, ProgressCallback is not the only superclass

            override fun progressChanged(percent: Double) {
                // ...
            }

            override fun compare(o1: Int?, o2: Int?): Int {
                TODO()
            }
        })

    val callbackWithState = object : ProgressCallback { // Compliant, object has properties and / or additional functions

        var state: Int = 0

        override fun progressChanged(percent: Double) {
            // ...
        }
    }

    val callbackWithAdditionalFun = object : ProgressCallback { // Compliant, object has properties and / or additional functions

        override fun progressChanged(percent: Double) {
            // ...
        }

        private fun compare(o1: Int?, o2: Int?): Int {
            TODO()
        }
    }

    val nonFunctionalCallback = object : NonFunctionalProgressCallback { // Compliant, interface is not a functional interface

        override fun progressChanged(percent: Double) {
            // ...
        }
    }

    object objectDeclaration : ProgressCallback { // Noncompliant {{Replace explicit functional interface implementation with lambda expression.}}
//  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

        override fun progressChanged(percent: Double) {
            // ...
        }

        class NestedClass {
            // ...
        }
    }
}

fun interface ProgressCallback {
    fun progressChanged(percent: Double)
}

interface NonFunctionalProgressCallback {
    fun progressChanged(percent: Double)
}

fun loadResource(callback: ProgressCallback) {
    // ...
}
