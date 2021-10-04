package checks

import kotlin.reflect.KClass

fun foo(arg: Any, kotlinClass: KClass<*>, javaClass: Class<*>): Int = when {
    String::class.isInstance(arg) -> 0 // Noncompliant {{Replace this usage of "isInstance" with "is String".}}
    //            ^^^^^^^^^^
    String::class.java.isInstance(arg) -> 1 // Noncompliant {{Replace this usage of "isInstance" with "is String".}}
    //                 ^^^^^^^^^^
    java.lang.String::class.java.isInstance(arg) -> 2 // Noncompliant {{Replace this usage of "isInstance" with "is java.lang.String".}}
    //                           ^^^^^^^^^^
    Boolean.javaClass.isInstance(arg) -> 3 // Noncompliant {{Replace this usage of "isInstance" with "is Boolean".}}
    //                ^^^^^^^^^^

    MyEnum::class.isInstance(arg) -> 3  // Noncompliant {{Replace this usage of "isInstance" with "is MyEnum".}}
    MyInterface::class.isInstance(arg) -> 3 // Noncompliant {{Replace this usage of "isInstance" with "is MyInterface".}}
    MyObject::class.isInstance(arg) -> 3 // Noncompliant {{Replace this usage of "isInstance" with "is MyObject".}}

    // Compliant, can't use "is" operator
    arg is String -> 4
    javaClass.javaClass.isInstance(arg) -> 5
    arg::class.isInstance(arg) -> 6
    kotlinClass.isInstance(arg) -> 7
    javaClass.isInstance(arg) -> 8
    String::class.isFinal::class.isInstance(arg) -> 9
    String()::class.isInstance(arg) -> 10

    // coverage
    with(javaClass.javaClass) { isInstance(arg) } -> -2

    else -> -1
}

enum class MyEnum {A,B}
interface MyInterface { }
object MyObject {}
