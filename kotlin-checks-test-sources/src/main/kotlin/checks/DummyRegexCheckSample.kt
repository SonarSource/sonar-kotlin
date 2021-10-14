package checks

val bar = Regex("foo" + "bar" + "\n" + """[a-z]""" + """test\nthis""") // Noncompliant {{Hello Test}}
//               ^^^     ^^^<    ^^<      ^^^^^<        ^^^^^^^^^^<

val bar2 = Regex("" + "" + """""") // Compliant - nothing we could raise on

val bar3 = "foo".toRegex() // Noncompliant
//          ^^^

val bar4 = ("foo" + "" + "bar").toRegex() // Noncompliant
//           ^^^          ^^^<

fun foo1(input: String) = Regex(input) // Compliant - we don't know what input is

private const val constant1 = "foo\nbar"
fun foo2(input: String) = Regex(input + "foo" + constant1) // FN Compliant - we can't resolve `input`, so we ignore entire regex

// Noncompliant@+3
private const val constant2 = "foo\nbar"
//                             ^^^^^^^^>
fun foo3(input: String) = Regex("foo" + constant2)
//                               ^^^
