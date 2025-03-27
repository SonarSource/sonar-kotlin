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

typealias JavaLangIllegalStateExceptionAlias  = java.lang.IllegalStateException
typealias KotlinIllegalStateExceptionAlias = kotlin.IllegalStateException
typealias KotlinIllegalStateExceptionAliasOfAlias = KotlinIllegalStateExceptionAlias

fun `Match aliased and non-aliased constructor`() {
    IllegalStateException("kotlin Alias implicit import")
    java.lang.IllegalStateException("java.lang FQN")
    JavaLangIllegalStateExceptionAlias("java.lang Alias")
    kotlin.IllegalStateException("kotlin FQN")
    KotlinIllegalStateExceptionAlias("kotlin Alias")
    KotlinIllegalStateExceptionAliasOfAlias("kotlin Alias of Alias")
    throw IllegalStateException("with throw")
}
