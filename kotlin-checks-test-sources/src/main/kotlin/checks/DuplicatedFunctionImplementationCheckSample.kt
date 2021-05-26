fun foobar() {}
fun foo_bar(): Int = 0 // Compliant - has no line

fun smallF1(b: Boolean) {
  val foo = 1
  val bar = foo > 3 || b
}

fun smallF2(b: Boolean) { // Noncompliant
  val foo = 1
  val bar = foo > 3 || b
}

fun smallF222(b: Boolean): Int {
  val foo = 1
  val bar = foo > 3 || b
  return 0
}
fun smallF333(b: Boolean): Long { // Сompliant, different return types
  val foo = 1
  val bar = foo > 3 || b
  return 0
}

fun f1(b: Boolean) {
//  ^^>
  val foobar = "abc"
  val foo = 1
  val bar = foo > 3 || b
}

fun f2(bar: Boolean): Int {
  val foobar = "abc"
  val foo = 1
  val baz = foo > 3 || bar
  return 0
}

fun f3(b: Boolean) { // Noncompliant {{Update this function so that its implementation is not identical to "f1" on line 25.}}
//  ^^
  val foobar = "abc"
  val foo = 1
  val bar = foo > 3 || b
}

fun f4(b: Boolean) { // Noncompliant {{Update this function so that its implementation is not identical to "f1" on line 25.}}
//  ^^
  val foobar = "abc"
  val foo = 1
  val bar = foo > 3 || b
}

fun f5(a: String, b: Boolean) { // Compliant - different parameter list
  val foobar = "abc";
  val foo = 1;
  val bar = foo > 3 || b
}

fun f6() {
  val foo = 1;
}

fun f7() { // Compliant - only 1 line
  val foo = 1;
}

fun f8(a: Int, b: Boolean) { // Compliant
  val foobar = "abc"
  val foo = 1
  val bar = foo > 3 || b
}

fun f9(a: Int, b: Boolean) { // Noncompliant
  val foobar = "abc"; val foo = 1; val bar = foo > 3 || b
}

fun f10(a: List<String>, b: Boolean) { // Compliant - not same parameter type
  val foobar = "abc"
  val foo = 1
  val bar = foo > 3 || b
}

fun ffun(a: Int, c: Int, b: Boolean) { // Compliant - not same parameters
  val foobar = "abc"
  val foo = 1
  val bar = foo > 3 || b
}

fun f11(a: Int, c: Int, b: Boolean) {
  val foobar = "abcdefg"
  val foo = 1
  val bar = foo > 3 || b
}

fun f12(a: Int, c: Int, b: Boolean) {
  val foobar = "abc"
  val foo = 2
  val bar = foo > 3 || b
}

class A {

  constructor(a: Int) {
    val foo = 1
    val bar = 2
  }

  fun constructor(a: Int) { // Compliant, constructor
    val foo = 1;
    val bar = 2;
  }

  fun protectedProperty() {
    data class C(protected val a: Int)

    val sf = testDefaultFactory()

    val testname = "${javaClass.name}\$${testName()}"
  }

  fun privateProperty() { // Compliant. property is private
    data class C(private val a: Int)

    val sf = testDefaultFactory()

    val testname = "${javaClass.name}\$${testName()}"
  }
  private fun testDefaultFactory(): Any {
    TODO()
  }

  private fun testName(): Any {
    TODO()
  }
}
