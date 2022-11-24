package checks

class  ArrayHashCodeAndToStringCheckSample {

    fun test(
        obj: Any,
        stringArray: Array<String>,
        stringList: List<String>,
        stringArrayOfArray: Array<Array<String>>,
        stringListOfArray: List<Array<String>>,
        booleanArray: BooleanArray,
        byteArray: ByteArray,
        shortArray: ShortArray,
        charArray: CharArray,
        intArray: IntArray,
        longArray: LongArray,
        floatArray: FloatArray,
        doubleArray: DoubleArray,
    ): String {
        val hash = obj.hashCode() +                     // Compliant
            stringArray.hashCode() +                    // Noncompliant {{Use "contentDeepHashCode()" instead.}}
            //          ^^^^^^^^^^
            stringArray.contentHashCode() +             // Compliant, with String elements, contentHashCode() is equivalent to contentDeepHashCode()
            stringArray.contentDeepHashCode() +         // Compliant
            stringList.hashCode() +                     // Compliant
            stringArrayOfArray.hashCode() +             // Noncompliant {{Use "contentDeepHashCode()" instead.}}
            stringArrayOfArray.contentHashCode() +      // Noncompliant {{Use "contentDeepHashCode()" instead.}}
            stringArrayOfArray.contentDeepHashCode() +  // Compliant
            stringListOfArray.hashCode() +              // Compliant
            booleanArray.hashCode() +                   // Noncompliant {{Use "contentHashCode()" instead.}}
            booleanArray.contentHashCode() +            // Compliant
            byteArray.hashCode() +                      // Noncompliant {{Use "contentHashCode()" instead.}}
            byteArray.contentHashCode() +               // Compliant
            shortArray.hashCode() +                     // Noncompliant {{Use "contentHashCode()" instead.}}
            shortArray.contentHashCode() +              // Compliant
            charArray.hashCode() +                      // Noncompliant {{Use "contentHashCode()" instead.}}
            charArray.contentHashCode() +               // Compliant
            intArray.hashCode() +                       // Noncompliant {{Use "contentHashCode()" instead.}}
            intArray.contentHashCode() +                // Compliant
            longArray.hashCode() +                      // Noncompliant {{Use "contentHashCode()" instead.}}
            longArray.contentHashCode() +               // Compliant
            floatArray.hashCode() +                     // Noncompliant {{Use "contentHashCode()" instead.}}
            floatArray.contentHashCode() +              // Compliant
            doubleArray.hashCode() +                    // Noncompliant {{Use "contentHashCode()" instead.}}
            doubleArray.contentHashCode()               // Compliant

        return "$hash" +
            obj.toString() +                            // Compliant
            stringArray.toString() +                    // Noncompliant {{Use "contentDeepToString()" instead.}}
            stringArray.contentToString() +             // Compliant, with String elements, contentToString() is equivalent to contentDeepToString()
            stringArray.contentDeepToString() +         // Compliant
            stringList.toString() +                     // Compliant
            stringArrayOfArray.toString() +             // Noncompliant {{Use "contentDeepToString()" instead.}}
            stringArrayOfArray.contentToString() +      // Noncompliant {{Use "contentDeepToString()" instead.}}
            stringArrayOfArray.contentDeepToString() +  // Compliant
            stringListOfArray.toString() +              // Compliant
            booleanArray.toString() +                   // Noncompliant {{Use "contentToString()" instead.}}
            booleanArray.contentToString() +            // Compliant
            byteArray.toString() +                      // Noncompliant {{Use "contentToString()" instead.}}
            byteArray.toString(Charsets.UTF_8) +        // Compliant
            byteArray.contentToString() +               // Compliant
            shortArray.toString() +                     // Noncompliant {{Use "contentToString()" instead.}}
            shortArray.contentToString() +              // Compliant
            charArray.toString() +                      // Noncompliant {{Use "contentToString()" instead.}}
            charArray.contentToString() +               // Compliant
            intArray.toString() +                       // Noncompliant {{Use "contentToString()" instead.}}
            intArray.contentToString() +                // Compliant
            longArray.toString() +                      // Noncompliant {{Use "contentToString()" instead.}}
            longArray.contentToString() +               // Compliant
            floatArray.toString() +                     // Noncompliant {{Use "contentToString()" instead.}}
            floatArray.contentToString() +              // Compliant
            doubleArray.toString() +                    // Noncompliant {{Use "contentToString()" instead.}}
            doubleArray.contentToString()               // Compliant
    }

    fun <T> testAllKindsOfTypeDeclaration(genericArgument: Array<Array<T>>) {
        val valInferredType = arrayOf(arrayOf("A", "B"), arrayOf("C", "D"))
        var varInferredType = arrayOf(arrayOf("A", "B"), arrayOf("C", "D"))
        val valExplicitType: Array<Array<String>> = arrayOf(arrayOf("A", "B"), arrayOf("C", "D"))
        var varExplicitType: Array<Array<String>> = arrayOf(arrayOf("A", "B"), arrayOf("C", "D"))
        println(genericArgument.contentToString())  // Noncompliant
        println(valInferredType.contentToString())  // Noncompliant
        println(varInferredType.contentToString())  // Noncompliant
        println(valExplicitType.contentToString())  // Noncompliant
        println(varExplicitType.contentToString())  // Noncompliant
    }

    fun rspecSamples() {
        val primitiveArray: IntArray = intArrayOf(1, 2, 3)
        val objectArray: Array<String> = arrayOf("A", "B", "C")
        val arrayOfArray: Array<Array<String>> = arrayOf(arrayOf("A", "B"), arrayOf("C", "D"))

        println(primitiveArray.toString())       // Noncompliant
        println(primitiveArray.hashCode())       // Noncompliant
        println(objectArray.toString())          // Noncompliant
        println(objectArray.hashCode())          // Noncompliant
        println(arrayOfArray.toString())         // Noncompliant
        println(arrayOfArray.contentToString())  // Noncompliant
        println(arrayOfArray.hashCode())         // Noncompliant
        println(arrayOfArray.contentHashCode())  // Noncompliant

        println(primitiveArray.contentToString())   // Compliant
        println(primitiveArray.contentHashCode())   // Compliant
        println(objectArray.contentDeepToString())  // Compliant
        println(objectArray.contentDeepHashCode())  // Compliant
        println(arrayOfArray.contentDeepToString()) // Compliant
        println(arrayOfArray.contentDeepHashCode()) // Compliant
    }

}
