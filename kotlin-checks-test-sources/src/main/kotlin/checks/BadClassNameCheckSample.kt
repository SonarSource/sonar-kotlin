package checks

import java.io.Serializable

class BadClassNameCheckSample {
}

class myClass{} // Noncompliant {{Rename class "myClass" to match the regular expression ^[A-Z][a-zA-Z0-9]*$.}}
//    ^^^^^^^

class My_Class{} // Noncompliant {{Rename class "My_Class" to match the regular expression ^[A-Z][a-zA-Z0-9]*$.}}

class my_class{} // Noncompliant {{Rename class "my_class" to match the regular expression ^[A-Z][a-zA-Z0-9]*$.}}

class MyClass1{} // Compliant

class MyClassC{} // Compliant

class MyClass_{} // Noncompliant {{Rename class "MyClass_" to match the regular expression ^[A-Z][a-zA-Z0-9]*$.}}

enum class Enum {
    MY_ENUM_1, // Compliant
    MY_ENUM_2, // Compliant
}

private fun anonymousClass() = object : Serializable {}

// TODO false-negative
object singleton
