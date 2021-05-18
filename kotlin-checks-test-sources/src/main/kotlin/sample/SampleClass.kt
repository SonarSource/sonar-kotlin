package sample

class SampleClass {
    fun sayHello(name: String) {
        println("Hello, $name!")
    }

    fun sayHelloNullable(name: String?) {
        println("Hello, ${name ?: "empty void"}!")
    }
}
