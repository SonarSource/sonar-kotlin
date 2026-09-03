package checks

@Deprecated("")
class DeprecatedCodeCheckSample { // Noncompliant {{Do not forget to remove this deprecated code someday.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^

  @Deprecated("")
  constructor(s: String) { // Noncompliant
//^^^^^^^^^^^
  }

  @Deprecated("")
  val prop: String = "" // Noncompliant
//    ^^^^

  var prop2: String = ""
    @Deprecated("") set // Noncompliant
//                  ^^^
  var prop3: String = ""
    @Deprecated("") get // Noncompliant
//                  ^^^

}

@Deprecated("")
fun deprecated() { // Noncompliant
//  ^^^^^^^^^^
}

@Deprecated("")
annotation class Deprecated2 // Noncompliant

class OK

@Deprecated("")
typealias KtString = String // Noncompliant
//        ^^^^^^^^

@Deprecated("")
private operator fun KtString.minus(s: String) = this + s // Noncompliant

class DeprecatedPrimaryConstructor @Deprecated("") constructor() { // Noncompliant
//                                                 ^^^^^^^^^^^
}

var anonymousFunction = @Deprecated("") fun() { // Noncompliant
//                      ^^^^^^^^^^^^^^^
}


var anonymousClass = @Deprecated("") object : Any() { // Noncompliant
//                   ^^^^^^^^^^^^^^^

}

// region override functions implementing external/parent APIs should not be flagged

open class BaseClass {
    open fun legacyMethod(): Unit = Unit
    open val legacyProp: String get() = ""
}

class DerivedClass : BaseClass() {
    @Deprecated("Deprecated in parent class")
    override fun legacyMethod(): Unit = Unit // Compliant - override, removal is not unilateral

    @Deprecated("Deprecated in Java")
    override val legacyProp: String get() = "" // Compliant - override property
}

fun interface LegacyInterface {
    @Deprecated("")
    fun interfaceMethod() // Noncompliant
//      ^^^^^^^^^^^^^^^
}

class InterfaceImpl : LegacyInterface {
    @Deprecated("Deprecated in Java")
    override fun interfaceMethod() {} // Compliant - override
}

// endregion

// region DeprecationLevel.HIDDEN and ERROR signals deliberate binary-compat lifecycle management

@Deprecated("Hidden for binary compat", level = DeprecationLevel.HIDDEN)
fun hiddenDeprecated() {} // Compliant

@Deprecated("Hidden", level = DeprecationLevel.HIDDEN)
class HiddenDeprecatedClass // Compliant

@Deprecated("DO NOT CALL - compile-time guard", level = DeprecationLevel.ERROR)
fun errorGuard() {} // Compliant

@Deprecated("Error", level = DeprecationLevel.ERROR)
class ErrorDeprecatedClass // Compliant

// Explicit DeprecationLevel.WARNING still reports
@Deprecated("Please migrate", level = DeprecationLevel.WARNING)
fun explicitWarningFunction() {} // Noncompliant
//  ^^^^^^^^^^^^^^^^^^^^^^^

//endregion
