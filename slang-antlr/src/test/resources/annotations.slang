@MyAnnotation
@MyAnnotation("Something")
@MyAnnotation(value = "Something")
@MyAnnotation(value = {"Something"})
@MyAnnotation(value = {"Something", "else"})
@MyAnnotation("something", "else")
@MyAnnotation("something", value = "else")
@MyAnnotation(value = "something", value = {"else", "and", "more"})
class {
    @MyFieldAnnotation
    int val x;

    @MyMethodAnnotation
    int fun test() {
        @MyLocalVariableAnnotation
        int val y;
    }

    int fun test(@MyParameterAnnotation int a) {}

    int fun foo(int a, @B int b, @C int c, int d) {}

    void fun bar(@Suppress("slang:S1764") int a = (1 == 1), int b = (1 == 1)) {}

}
