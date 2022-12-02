package checks

class CollectionSizeAndArrayLengthCheckSample {

    fun noncompliant(intArray: Array<Int>, intColl: Collection<Int>) {
        if (intColl.size >= 0) { // Noncompliant {{The size of an array/collection is always ">=0", update this test to either ".isNotEmpty()" or ".isEmpty()".}}
            println("test")
        }
        if (intColl.size < 0) { // Noncompliant {{The size of an array/collection is never "<0", update this test to use ".isEmpty()".}}
            println("test")
        }

        if (0 > intArray.size) { // Noncompliant
            println("test")
        }
        if (0 > intColl.size) { // Noncompliant
            println("test")
        }

        if (-1 == intArray.size) { // Noncompliant
            println("test")
        }
        if (intColl.size == -1) { // Noncompliant
            println("test")
        }
    }

    fun compliant(intArray: Array<Int>, intColl: Collection<Int>) {
        if (intArray.size >= 3) {
            println("test")
        }
        if (intArray.size == 0) {
            println("test")
        }
        if (0 < intArray.size) {
            println("test")
        }
        if (2 > intColl.size) {
            println("test")
        }
    }

}
