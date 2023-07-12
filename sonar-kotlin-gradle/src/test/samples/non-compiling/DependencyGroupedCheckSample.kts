dependencies {
    compile("org.foo:bar") // Compliant
    implementation("org.foo:bar") // Compliant
    compile("org.foo:bar") // Noncompliant {{Group `compile` dependencies}}
//  ^^^^^^^
    compile("org.foo:bar") // Noncompliant {{Group `compile` dependencies}}
//  ^^^^^^^
    implementation("org.foo:bar") // Compliant

    compileClasspath("org.foo:bar") // Complaint

    compileOnly("org.foo:bar") // Compliant
    implementation("org.foo:bar") // Noncompliant {{Group `implementation` dependencies}}
//  ^^^^^^^^^^^^^^

    println("Hello, world!") // Compliant
    compileOnly("org.foo:bar") // Compliant
    compileClasspath("org.foo:bar") // Noncompliant {{Group `compileClasspath` dependencies}}

    compileOnly("org.foo:bar") // Compliant
    runtime("org.foo:bar") // Compliant
    runtime("org.foo:bar") // Compliant
    println("Hello, world!") // Compliant
    println("Hello, world!") // Compliant
    runtime("org.foo:bar") // Compliant
    runtime("org.foo:bar") // Compliant
    compileOnly("org.foo:bar") // Noncompliant {{Group `compileOnly` dependencies}}

    constraints {
        compile("org.foo:bar") // Compliant
        implementation("org.foo:bar") // Compliant
        compile("org.foo:bar") // Noncompliant {{Group `compile` dependencies}}
//      ^^^^^^^
        compile("org.foo:bar") // Noncompliant {{Group `compile` dependencies}}
        implementation("org.foo:bar") // Compliant

        compileClasspath("org.foo:bar") // Complaint

        compileOnly("org.foo:bar") // Compliant
        implementation("org.foo:bar") // Noncompliant {{Group `implementation` dependencies}}
        println("Hello, world!") // Compliant
        compileOnly("org.foo:bar") // Compliant
        compileClasspath("org.foo:bar") // Noncompliant {{Group `compileClasspath` dependencies}}
    }

    compileClasspath("org.foo:bar") // Noncompliant {{Group `compileClasspath` dependencies}}
    runtime("org.foo:bar") // Compliant
    runtime("org.foo:bar") // Compliant
}

dependencies {
    compile("org.foo:bar") // Compliant
    compile("org.foo:bar") // Compliant
    compile("org.foo:bar") // Compliant
    implementation("org.foo:bar") // Compliant
    implementation("org.foo:bar") // Compliant
    implementation("org.foo:bar") // Compliant
    compileClasspath("org.foo:bar") // Complaint
    compileClasspath("org.foo:bar") // Compliant
    compileOnly("org.foo:bar") // Compliant

    println("Hello, world!") // Compliant
    compileOnly("org.foo:bar") // Compliant

    compileOnly("org.foo:bar") // Compliant
    compileOnly("org.foo:bar") // Compliant
    runtime("org.foo:bar") // Compliant
    runtime("org.foo:bar") // Compliant
    println("Hello, world!") // Compliant
    println("Hello, world!") // Compliant
    runtime("org.foo:bar") // Compliant
    runtime("org.foo:bar") // Compliant

    constraints {
        compile("org.foo:bar") // Compliant
        compile("org.foo:bar") // Compliant
        compile("org.foo:bar") // Compliant
        implementation("org.foo:bar") // Compliant
        implementation("org.foo:bar") // Compliant
        implementation("org.foo:bar") // Compliant

        compileClasspath("org.foo:bar") // Complaint
        compileClasspath("org.foo:bar") // Compliant

        compileOnly("org.foo:bar") // Compliant
        println("Hello, world!") // Compliant
        compileOnly("org.foo:bar") // Compliant
    }

    runtime("org.foo:bar") // Compliant
    runtime("org.foo:bar") // Compliant
}

foo {
    compile("org.foo:bar") // Compliant
    implementation("org.foo:bar") // Compliant
    compile("org.foo:bar") // Compliant

    constraints {
        compile("org.foo:bar") // Compliant
        implementation("org.foo:bar") // Compliant
        compile("org.foo:bar") // Compliant
    }
}

constraints {
    compile("org.foo:bar") // Compliant
    implementation("org.foo:bar") // Compliant
    compile("org.foo:bar") // Compliant
}
