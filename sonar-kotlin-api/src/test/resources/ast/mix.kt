package org.sonarsource.kotlin.converter.ast

data class B(val f2: Int)

enum class MyEnum {
    SUNDAY, MONDAY
}

fun a() {
    fun a() {
        while (true);
    }
}

class A(var a: String) {
    fun b(a: String) {
        this.a = ""
    }
}
