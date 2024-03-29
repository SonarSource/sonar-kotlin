package sample

class SampleClass {
    fun sayHello(name: String) {
        println("Hello, $name!")
    }

    fun sayHelloNullable(name: String?): Int {
        println("Hello, ${name ?: "empty void"}!")
        return 0
    }

    fun intAndVararg(one: Int, vararg two: String) {
        // empty
    }

    operator fun get(index: Int) = 42
}

suspend fun String.suspendExtFun(): String = ""
