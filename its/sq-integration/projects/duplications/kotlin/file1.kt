package com.example.mypackage

import com.example.somewhere.MyClass

fun function1(args: Array<String>) {
    println("hello world1")
    var a = 0
    a = a + 1
    println("hello world2")
    var a = 0
    a = a + 1
    println("hello world3")
    var a = 0
    a = a + 1
    println("hello world4")
    var a = 0
    a = a + 1
    println("hello world1")
    var a = 0
    a = a + 1
    println("hello world2")
    var a = 0
    a = a + 1
    println("hello world3")
    var a = 0
    a = a + 1
    println("hello world4")
    var a = 0
    a = a + 1
}
// 26 previous lines are a duplication of function1 in file2.kt
// It counts as 2 blocks since the whole function is duplicated by function1 (in file2.kt), and the body by function2.

fun function2(args: Array<String>) {
    println("other string 1")
    var a = 0
    a = a + 1
    println("other string 2")
    var a = 0
    a = a + 1
    println("other string 3")
    var a = 0
    a = a + 1
    println("other string 4")
    var a = 0
    a = a + 1
    println("other string 1")
    var a = 0
    a = a + 1
    println("other string 2")
    var a = 0
    a = a + 1
    println("other string 3")
    var a = 0
    a = a + 1
    println("other string 4")
    var a = 0
    a = a + 1
}
// 25 previous lines (function signature is different) are a duplication of the body of function1

fun function3(args: Array<String>) {
    println("")
    var a = 0
    a = a + 2
    println("")
    var a = 0
    a = a + 3
    println("")
    var a = 0
    a = a + 4
    println("")
    var a = 0
    a = a + 5
    println("")
    var a = 0
    a = a + 6
    println("")
    var a = 0
    a = a + 7
    println("")
    var a = 0
    a = a + 8
    println("")
    var a = 0
    a = a + 9
}
