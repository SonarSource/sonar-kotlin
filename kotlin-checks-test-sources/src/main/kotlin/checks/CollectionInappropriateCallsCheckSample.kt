package checks

class CollectionInappropriateCallsCheckSample {

    val intMap = mutableMapOf<Int, String>()
    val numMap = mutableMapOf<Number?, Number>()
    val intArr = arrayOf<Int>()
    val intList = listOf<Int>(1, 2, 3)
    val strList = listOf<String>("1", "2", "3")
    val intMutableCollection = mutableListOf<Int>(2, 3, 4)
    val nullableIntArray = arrayOf<Int?>()

    fun getListOfStrings() = listOf<String>()

    val lazy by lazy {
        "return "
    }

    val actualLazy = lazy {
        "asd"
    }

    fun testBy(){
        strList.indexOf<Any>(lazy) // Compliant
        strList.indexOf<Any>(actualLazy) // Noncompliant
    }

    class StringListProvider {
        companion object {
            fun get() = listOf<String>()
        }
    }

    fun <T: String> getT(arg: List<T>): T{
        arg.contains<Any>(1) // Compliant FN: For now we don't support unresolved generics.
        return arg.first()
    }

    fun <T: Any> getT2(arg: List<Int>, t :T){
        arg.contains<Any>(t) // Compliant FN: For now we don't support unresolved generics.
    }

    fun <T: String> getT2(arg: List<Int>, t :T){
        arg.contains<Any>(t) // Compliant FN: For now we don't support unresolved generics.
    }

    val params = listOf<Int>(1,2,3)
    private val _parameters: Array<Int> = params.toTypedArray()
    fun getParameters() = _parameters
    fun getParameterIndex(parameter: Int) = _parameters.indexOf(parameter)

    fun noncompliant(int: Int, number: Number, str: String) {

        intMap.containsValue<Int, Any>(1) // Noncompliant
        numMap.remove<Any?, Number>("string") // Noncompliant {{This key/object cannot ever be present in the collection}}
        with(intMap) {
            this.remove<Any, String>("2") // Noncompliant
        }
        var s = "string"
        intMap.remove<Any, String>(s)// Noncompliant
        intMap.remove<Any, String>({ "string" })// Noncompliant

        intArr.lastIndexOf<Any>(str) // Noncompliant
        intArr.lastIndexOf<Any>({ "string" }) // Noncompliant

        intMutableCollection.removeAll<Any>(strList) // Noncompliant
        intMutableCollection.removeAll<Any>(listOf("1")) // Noncompliant
        intMutableCollection.retainAll<Any>(StringListProvider.get()) // Noncompliant
        intMutableCollection.retainAll<Any>(getListOfStrings()) // Noncompliant
        intMutableCollection.containsAll<Any>(getListOfStrings()) // Noncompliant

        intMap.containsKey<Any>(strList[0]) // Noncompliant
        intMap.containsValue<Int, Any>(1) // Noncompliant

        intMap.get<Any, String>("string") // Noncompliant

        StringListProvider.get().indexOf<Any>(1) // Noncompliant

    }

    fun compliant(int: Int, number: Number, any: Any, nullable: Int?) {
        intMap.remove(nullable)
        nullableIntArray.indexOf(int)
        intMap.remove(number) // Compliant: Number might be an Int, and we want to avoid FPs
        intMap.remove<Any, String>(any)
        numMap.remove<Any?, Number>(int) // Compliant: we can use map key subtypes as arguments
        val x = 2
        intMap.remove<Any, String>(x) // Compliant

        intArr.indexOf<Any>(1)
        intArr.indexOf(number)
        intArr.contains(number)
        intArr.lastIndexOf<Any>(number)

        intMutableCollection.removeAll<Any>(intList)
        strList.indexOf<Any>(any.toString())

        intMap.get<Any, String>(number)
        intMap.get<Any, String>(any)

        val myMap = mapOf<List<String>, String>()
        val element = listOf<String>()
        myMap.contains(element)
    }

}
