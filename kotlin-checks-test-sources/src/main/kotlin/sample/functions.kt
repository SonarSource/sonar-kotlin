package sample

suspend fun main() {
    val sampleClass = SampleClass()
    sampleClass.sayHello("Kotlin")
    sampleClass.sayHello("Java")
    sampleClass.sayHelloNullable("nothingness")
    "".suspendExtFun()

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
