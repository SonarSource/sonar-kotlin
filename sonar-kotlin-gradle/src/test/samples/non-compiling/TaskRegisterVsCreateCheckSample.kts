tasks.create("foo1") { // Noncompliant {{Use `tasks.register(...)` instead.}}
//    ^^^^^^
}

tasks.create("foo2", JavaCompile::class) { // Noncompliant

}

tasks.create("foo3", JavaCompile::class.java) { // Noncompliant

}

tasks.create<JavaCompile>("foo4") { // Noncompliant

}

tasks.register("bar1") {

}

tasks.register("bar2", JavaCompile::class) {

}

tasks.register("bar3", JavaCompile::class.java) {

}

tasks.register<JavaCompile>("bar4") {

}

somethingElse.create("abc1") { // Compliant - we ignore anything not called on 'tasks' for now.

}

whith(tasks) {
    create("abc2a") // Compliant - as long as we don't have semantics, we ignore any construct that is not exactly 'tasks.create...'
    this.create("abc2b")
}

create("abc3") {

}
