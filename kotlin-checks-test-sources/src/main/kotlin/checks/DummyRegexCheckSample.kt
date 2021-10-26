package checks

import java.util.regex.Pattern
import kotlin.text.RegexOption.LITERAL as RENAMED_LITERAL

val bar = Regex("foo" + "bar" + "\n" + """[a-z]""" + """test\nthis""") // Noncompliant {{Flags: 0}}
//               ^^^ 5
//                       ^^^@-1< {{Flags: 0}}
//                               ^^@-2< {{Flags: 0}}
//                                        ^^^^^@-3< {{Flags: 0}}
//                                                      ^^^^^^^^^^@-4< {{Flags: 0}}
//        ^^^^^@-5< {{Function call of which the argument is interpreted as regular expression.}}


val bar2 = Regex("" + "" + """""") // Compliant - nothing we could raise on

val bar3 = "foo".toRegex() // Noncompliant
//          ^^^ 1^^^^^^^<

val bar4 = ("foo" + "" + "bar").toRegex() // Noncompliant
//           ^^^ 2        ^^^<  ^^^^^^^<

fun foo1(input: String) = Regex(input) // Compliant - we don't know what input is

private const val constant1 = "foo\nbar"
fun foo2(input: String) = Regex(input + "foo" + constant1) // FN Compliant - we can't resolve `input`, so we ignore entire regex

// Noncompliant@+3
private const val constant2 = "foo\nbar"
//                             ^^^^^^^^>
fun foo3(input: String) = Regex("foo" + constant2)
//                        ^^^^^> ^^^ 2

val bar5a = Regex("some regex", RegexOption.IGNORE_CASE) // Noncompliant {{Flags: 66}}
//                 ^^^^^^^^^^
val bar5b = "some regex".toRegex(RegexOption.IGNORE_CASE) // Noncompliant {{Flags: 66}}
//           ^^^^^^^^^^ 1^^^^^^^<

val bar6a = Regex("some regex", setOf(RegexOption.LITERAL, RegexOption.IGNORE_CASE, RegexOption.IGNORE_CASE, RegexOption.COMMENTS)) // Noncompliant {{Flags: 86}}
val bar6b = "some regex".toRegex(setOf(RegexOption.LITERAL, RegexOption.IGNORE_CASE, RegexOption.IGNORE_CASE, RegexOption.COMMENTS)) // Noncompliant {{Flags: 86}}

val singleFlag = RegexOption.UNIX_LINES
val bar7 = Regex("regex", setOf(singleFlag)) // Noncompliant {{Flags: 1}}

val multipleFlags = setOf(RegexOption.LITERAL, RegexOption.UNIX_LINES)
val bar8 = Regex("regex", multipleFlags) // Noncompliant {{Flags: 17}}

// Noncompliant@+1 {{Flags: 1}}
val someString = "foo"
//                ^^^
val someString2 = "bar"

val bar9a = Regex(someString, singleFlag)
//          ^^^^^<

// Noncompliant@-5 {{Flags: 17}}
val bar9b = Regex(someString2, multipleFlags)

val p1 = Pattern.compile("regex", Pattern.UNICODE_CASE) // Noncompliant {{Flags: 64}}
val p2 = Pattern.compile("regex", Pattern.UNICODE_CASE or Pattern.UNIX_LINES) // Noncompliant {{Flags: 65}}
// The following is mis-detecting the flags, we don't actually analyze how the flags are combined for now
val p3 = Pattern.compile("regex", Pattern.UNICODE_CASE and Pattern.UNIX_LINES) // Noncompliant {{Flags: 65}}

val bar10 = Regex("foo", RENAMED_LITERAL) // Noncompliant {{Flags: 16}}
