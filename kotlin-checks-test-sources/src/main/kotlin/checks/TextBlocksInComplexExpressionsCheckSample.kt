package checks

class TextBlocksInComplexExpressionsCheckSample {

    fun exceptions(){
        listOfStrings.forEach{
                        """<parent>
                        <anothertag>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>
                          <tag>
                          <test>"""
        }
    }

    val increment = { list: List<String> -> println("""
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
            .map ({ str ->
                listOfStrings.forEach{
                    foo( // Noncompliant@+1
                        """<parent>
                        <anothertag>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>
                          <tag>
                          <test>"""
                    )
                }
                foo( // Noncompliant@+1
                        """<parent>
                        <anothertag>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>
                          <tag>
                          <test>"""
                    )
            })
    }

    fun simpleLambda() {

        listOfStrings.forEach {

            println(  // Noncompliant@+1
                """<parent>
                        <anothertag>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>
                          <tag>
                          <test>"""
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

    var res = """<project>
                          <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>"""


    fun foo(str: String) {
        str.split(":")
    }


    fun test4() {


        listOfStrings
            .map { str ->
                str != res +
                    """<parent>
                        <anothertag>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>""" +
                    res +
                    """
                        test me"""
            }


        listOfStrings
            .map { str ->
                str != res +
                    """<parent>
                        <anothertag>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>"""
            }


        listOfStrings
            .map { str ->
                str != res + // Noncompliant@+1
                    """<parent>
                        <anothertag>
                        </anothertag>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>"""
            }


        listOfStrings
            .map { str ->
                str != """<project>
                          <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>""" +
                    """<parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>"""
            }


        listOfStrings
            .map { str -> // Noncompliant@+1 {{Move this text block out of the lambda body and refactor it to a local variable or a static final field.}}
                str != """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                          </parent>
                
                          <groupId>com.mycompany.app</groupId>
                          <artifactId>my-module</artifactId>
                          <version>1</version>
                        </project>
                        """
            }



        listOfStrings.map { str ->
            var b = "not using multiline string literal"
            var c = "this should not raise and issue"
            var d = "even if the body of the lambda has more"
            var e = "than 5"
            var f = "lines of"
            println("code")
        }
    }


    fun test3() {
        listOfStrings.map { str -> // Noncompliant@+1
            var b = !"""
                        <projectB>
                          <modelVersion>B</modelVersion>
                          <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                          </parent>
                
                          <groupId>com.mycompany.app</groupId>
                          <artifactId>my-module</artifactId>
                          <version>1</version>
                        </projectB>
                    """.equals(str)
            println("ABC")


            var c = ! // Noncompliant@+1
            """
                        <projectC>
                          <modelVersion>C</modelVersion>
                          <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                          </parent>
                
                          <groupId>com.mycompany.app</groupId>
                          <artifactId>my-module</artifactId>
                          <version>1</version>
                        </projectC>
                    """.equals(str)

            return
        }
    }

    fun test() {

        listOfStrings
            .map { str ->
                str != """<project>
                          <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                          </parent>"""
            }
    }

    var myTextBlock = """
                        <project>
                          <modelVersion>4.0.0</modelVersion>
                          <parent>
                            <groupId>com.mycompany.app</groupId>
                            <artifactId>my-app</artifactId>
                            <version>1</version>
                          </parent>
                
                          <groupId>com.mycompany.app</groupId>
                          <artifactId>my-module</artifactId>
                          <version>1</version>
                        </project>
                    """

    fun test2() {
        listOfStrings.map { str ->
            !myTextBlock.equals(str) // Compliant
        }

        listOfStrings.map { str ->
            "ABC\\nABC\\nABC\\nABC\\nABC\\nABC" // Compliant
        }

    }
}
