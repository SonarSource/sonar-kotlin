package sonar.rocks

import the.best.library.LibClass

class MyFirstClass {
    companion object {
        const val cfoo1: Int = 1
        val cfoo2 = 2
        var cfoo3 = 3
        const val cfoo4 = "This is a String!"
        var cfoo5 = "This is also a String"
        val cfoo6 = "This includes another String ${'$'}cfoo4"
    }

    /**
     *  This is a super informative doc comment
     */
    fun main(args: Array<String>) {
        print (1 == 1); print("abc");

        cfoo5.let {
            println(it)
        }

        with(cfoo6) {
            println(this)
            println(this);
        }
    }

    @Throws(java.io.IOException::class)
    private fun `cool functionality with unusual name`() {
        // do nothing
    }

    override fun foo() {} // for testing if override is highlighted
}
data class A(val a: Int)
