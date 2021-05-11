package sample

fun main() {
    val sampleClass = SampleClass()
    sampleClass.sayHello("Kotlin")
    sampleClass.sayHello("Java")

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