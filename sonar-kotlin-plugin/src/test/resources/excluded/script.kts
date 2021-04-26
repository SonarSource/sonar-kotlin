@file:Repository("https://jcenter.bintray.com")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-html-jvm:0.6.11")

import kotlinx.html.*
import kotlinx.html.stream.*

print(createHTML().html {
    body {
        h1 { +"Hello, World!" }
    }
})
