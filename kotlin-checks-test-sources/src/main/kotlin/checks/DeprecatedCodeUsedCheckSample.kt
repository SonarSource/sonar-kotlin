package checks

@Deprecated("")
interface Old

class Example : Old // Noncompliant {{Deprecated code should not be used.}}
//              ^^^
