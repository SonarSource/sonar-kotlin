package sample

suspend fun main() {
    val sampleClass = SampleClass()
    sampleClass.sayHello("Kotlin")
    sampleClass.sayHello("Java")
    sampleClass.sayHelloNullable("nothingness")
    "".suspendExtFun()
    sampleClass.intAndVararg(42, "one")
    sampleClass.intAndVararg(42, "one", "two")
    sampleClass.get(42)

    val mySampleClass = MySampleClass()
}

class MySampleClass : MyInterface {
    override fun sayHello(name: String) {
        println("Hello, $name!")
    }
}

interface MyInterface {
    fun sayHello(name: String)
}
