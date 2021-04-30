package sample

fun main() {
    val sampleClass = SampleClass()
    sampleClass.sayHello("Kotlin")
    sampleClass.sayHello("Java")
}

class MySampleClass {
    fun sayHello(name: String) {
        println("Hello, $name!")
    }

}
