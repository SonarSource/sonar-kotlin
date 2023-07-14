dependencies { // Noncompliant {{Group dependencies by their destination.}}
//^[sc=1;ec=12]
    compile("org.foo:bar")
    implementation("org.foo:bar")
    compile("org.foo:bar")
//  ^^^^^^^<
    compile("org.foo:bar")
//  ^^^^^^^<
    implementation("org.foo:bar")

    compileClasspath("org.foo:bar")

    compileOnly("org.foo:bar")
    implementation("org.foo:bar")
//  ^^^^^^^^^^^^^^<

    println("Hello, world!")
    compileOnly("org.foo:bar")
    compileClasspath("org.foo:bar")
//  ^^^^^^^^^^^^^^^^<

    compileOnly("org.foo:bar")
    runtime("org.foo:bar")
    runtime("org.foo:bar")
    println("Hello, world!")
    println("Hello, world!")
    runtime("org.foo:bar")
    runtime("org.foo:bar")
    compileOnly("org.foo:bar")
//  ^^^^^^^^^^^<

    constraints { // Noncompliant {{Group dependencies by their destination.}}
//  ^^^^^^^^^^^
        compile("org.foo:bar")
        implementation("org.foo:bar")
        compile("org.foo:bar")
//      ^^^^^^^<
        compile("org.foo:bar")
//      ^^^^^^^<
        implementation("org.foo:bar")

        compileClasspath("org.foo:bar")

        compileOnly("org.foo:bar")
        implementation("org.foo:bar")
//      ^^^^^^^^^^^^^^<
        println("Hello, world!")
        compileOnly("org.foo:bar")
        compileClasspath("org.foo:bar")
//      ^^^^^^^^^^^^^^^^<
    }

    runtime("org.foo:bar")
    runtime("org.foo:bar")
}

dependencies { // Compliant
    compile("org.foo:bar")
    compile("org.foo:bar")
    compile("org.foo:bar")
    implementation("org.foo:bar")
    implementation("org.foo:bar")
    implementation("org.foo:bar")
    compileClasspath("org.foo:bar")
    compileClasspath("org.foo:bar")
    compileOnly("org.foo:bar")

    println("Hello, world!")
    compileOnly("org.foo:bar")

    compileOnly("org.foo:bar")
    compileOnly("org.foo:bar")
    runtime("org.foo:bar")
    runtime("org.foo:bar")
    println("Hello, world!")
    println("Hello, world!")
    runtime("org.foo:bar")
    runtime("org.foo:bar")

    constraints {
        compile("org.foo:bar")
        compile("org.foo:bar")
        compile("org.foo:bar")
        implementation("org.foo:bar")
        implementation("org.foo:bar")
        implementation("org.foo:bar")

        compileClasspath("org.foo:bar")
        compileClasspath("org.foo:bar")

        compileOnly("org.foo:bar")
        println("Hello, world!")
        compileOnly("org.foo:bar")
    }

    runtime("org.foo:bar")
    runtime("org.foo:bar")
}

fun foo(configuration: DependencyHandlerScope.() -> Unit) {}
fun constraints(configuration: DependencyHandlerScope.() -> Unit) {}

foo { // Compliant
    compile("org.foo:bar")
    implementation("org.foo:bar")
    compile("org.foo:bar")

    constraints {
        compile("org.foo:bar")
        implementation("org.foo:bar")
        compile("org.foo:bar")
    }
}

constraints { // Compliant
    compile("org.foo:bar")
    implementation("org.foo:bar")
    compile("org.foo:bar")
}
