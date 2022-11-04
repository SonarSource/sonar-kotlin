package checks

class TextBlocksInComplexExpressionsCheckSampleSingleLines {

    fun foo(str: String) {
        str.split(":")
    }

    val increment = { _: List<String> -> println("""
        this multi
        line string
        should not 
        raise an issue
        because the lambda is not
        a parameter
    """.trimIndent()) }


    val listOfStrings = listOf("1", "2", "3")

    fun test5() {
        listOfStrings
            .map ({ _ ->
                listOfStrings.forEach{
                    foo( // Noncompliant@+1
                        """<parent>
                          <test>"""
                    )
                }
                foo( // Noncompliant@+1
                        """<parent>
                          <test>"""
                    )
            })
    }

    fun simpleLambda() {
        listOfStrings.forEach {

            println(
                """<parent>t><test>""" // Compliant
            )

            listOf("test").forEach {
                println(  // Noncompliant@+1
                    """<parent>
                        <anothertag>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>
                          <tag>
                          <test>"""
                )
            }
        }
    }


}
