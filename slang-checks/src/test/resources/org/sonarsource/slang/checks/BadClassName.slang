class myClass{} // Noncompliant {{Rename class "myClass" to match the regular expression ^[A-Z][a-zA-Z0-9]*$.}}
//    ^^^^^^^

class My_Class{} // Noncompliant {{Rename class "My_Class" to match the regular expression ^[A-Z][a-zA-Z0-9]*$.}}

class my_class{} // Noncompliant {{Rename class "my_class" to match the regular expression ^[A-Z][a-zA-Z0-9]*$.}}

class MyClass{} // Compliant

class MyClassC{} // Compliant

class MyClass${} // Noncompliant {{Rename class "MyClass$" to match the regular expression ^[A-Z][a-zA-Z0-9]*$.}}
