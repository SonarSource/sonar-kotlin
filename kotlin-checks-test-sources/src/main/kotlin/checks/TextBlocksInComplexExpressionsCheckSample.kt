package checks

class TextBlocksInComplexExpressionsCheckSample {

    val listOfStrings = listOf("1", "2", "3")


    fun test4(){

        // Compliant
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

        listOfStrings.map { str ->
            var b = "not using multiline string literal"
            var c = "this should not raise and issue"
            var d = "even if the body of the lambda has more"
            var e = "than 5"
            var f = "lines of"
            println("code")
        }
    }

    // Noncompliant@+3
    fun test3(){
        listOfStrings.map {str ->
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

            // Noncompliant@+2
            var c = !
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
        // Noncompliant@+3 {{Move this text block out of the lambda body and refactor it to a local variable or a static final field.}}
        listOfStrings
            .map { str ->
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

        // Compliant@+3
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

    fun test2(){
        listOfStrings.map {
            str -> !myTextBlock.equals(str) // Compliant
        }

        listOfStrings.map {
            str -> "ABC\\nABC\\nABC\\nABC\\nABC\\nABC" // Compliant
        }

    }
}
