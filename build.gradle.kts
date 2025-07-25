import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.sonarsource.kotlin.buildsrc.tasks.CreateKotlinGradleRuleStubsTask
import org.sonarsource.kotlin.buildsrc.tasks.CreateKotlinRuleStubsTask
import org.sonarsource.kotlin.buildsrc.tasks.FetchRuleMetadata

plugins {
    java
    id("jacoco")
    id("com.jfrog.artifactory") version "5.2.5"
    id("org.sonarqube") version "5.1.0.4882"
    id("org.jetbrains.kotlin.jvm") apply false
    id("com.diffplug.spotless") version "6.11.0"
    `maven-publish`
    signing
}

val projectTitle: String by project

configure(subprojects.filter { it.name != "kotlin-checks-test-sources" }) {
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {

        lineEndings = com.diffplug.spotless.LineEnding.UNIX

        fun SourceSet.findSourceFilesToTarget() = allJava.srcDirs.flatMap { srcDir ->
            project.fileTree(srcDir).filter { file ->
                file.name.endsWith(".kt") || (file.name.endsWith(".java") && file.name != "package-info.java")
            }
        }

        kotlin {
            licenseHeaderFile(rootProject.file("LICENSE_HEADER")).updateYearWithLatest(true)

            target(
                project.sourceSets.main.get().findSourceFilesToTarget(),
                project.sourceSets.test.get().findSourceFilesToTarget()
            )
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint()
        }

        format("misc") {
            // define the files to apply `misc` to
            target("*.gradle", "*.md", ".gitignore")

            // define the steps to apply to those files
            trimTrailingWhitespace()
            indentWithSpaces()
            endWithNewline()
        }
    }
}

allprojects {
    apply<JavaPlugin>()
    apply(plugin = "jacoco")
    apply(plugin = "com.jfrog.artifactory")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    gradle.projectsEvaluated {
        tasks.withType<JavaCompile> {
            if (project.hasProperty("warn")) {
                options.compilerArgs = options.compilerArgs + "-Xlint:unchecked" + "-Xlint:deprecation"
            } else {
                options.compilerArgs = options.compilerArgs + "-Xlint:-unchecked" + "-Xlint:-deprecation"
            }
        }
    }

    val buildNumber: String? = System.getProperty("buildNumber")

    ext {
        set("buildNumber", buildNumber)
    }

    // Replaces the version defined in sources, usually x.y-SNAPSHOT, by a version identifying the build.
    if (project.version.toString().endsWith("-SNAPSHOT") && buildNumber != null) {
        val versionSuffix =
            if (project.version.toString().count { it == '.' } == 1) ".0.$buildNumber" else ".$buildNumber"
        project.version = project.version.toString().replace("-SNAPSHOT", versionSuffix)
    }

    repositories {
        mavenCentral()
        maven(url = "https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") {
            content {
                includeGroup("org.jetbrains.kotlin")
            }
        }
    }
}

subprojects {
    val javadoc: Javadoc by tasks

    // do not publish to Artifactory by default
    tasks.artifactoryPublish {
        skip = true
    }

    java.sourceCompatibility = JavaVersion.VERSION_11
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(java.sourceCompatibility.majorVersion.toInt())
    }

    tasks.withType<KotlinCompile>().all {
        compilerOptions.jvmTarget = JvmTarget.fromTarget(java.sourceCompatibility.toString())
    }

    jacoco {
        toolVersion = "0.8.13"
    }

    tasks.jacocoTestReport {
        reports {
            xml.required.set(true)
            csv.required.set(false)
            html.required.set(false)
        }
    }

    // when subproject has Jacoco pLugin applied we want to generate XML report for coverage
    plugins.withType<JacocoPlugin> {
        tasks["test"].finalizedBy("jacocoTestReport")
    }

    configurations {
        // include compileOnly dependencies during test
        testImplementation {
            extendsFrom(configurations.compileOnly.get())
        }
    }

    if (!project.path.startsWith(":its") && !project.path.startsWith(":private:its")) {
        tasks.test {
            useJUnitPlatform()
        }
    }

    tasks.test {
        testLogging {
            exceptionFormat =
                org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL // log the full stack trace (default is the 1st line of the stack trace)
            events("skipped", "failed") // verbose log for failed and skipped tests (by default the name of the tests are not logged)
        }

        systemProperties = System.getProperties().filterKeys {
            it is String &&
                (it.startsWith("orchestrator") || it.startsWith("sonar") || it == "buildNumber" || it == "slangVersion")
        }.mapKeys { it.key as String }

        if (systemProperties.containsKey("buildNumber") && !systemProperties.containsKey("slangVersion")) {
            systemProperties["slangVersion"] = version
        }
    }

    val sourcesJar by tasks.creating(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    val javadocJar by tasks.creating(Jar::class) {
        dependsOn(javadoc)
        archiveClassifier.set("javadoc")
        from(tasks["javadoc"])
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                pom {
                    name.set(projectTitle)
                    description.set(project.description)
                    url.set("http://www.sonarqube.org/")
                    organization {
                        name.set("SonarSource")
                        url.set("http://www.sonarqube.org/")
                    }
                    licenses {
                        license {
                            name.set("SSALv1")
                            url.set("https://sonarsource.com/license/ssal/")
                            distribution.set("repo")
                        }
                    }
                    scm {
                        url.set("https://github.com/SonarSource/sonar-kotlin")
                    }
                    developers {
                        developer {
                            id.set("sonarsource-team")
                            name.set("SonarSource Team")
                        }
                    }
                }
            }
        }
    }

    signing {
        val signingKeyId: String? by project
        val signingKey: String? by project
        val signingPassword: String? by project
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        setRequired {
            val branch = System.getenv()["CIRRUS_BRANCH"] ?: ""
            (branch == "master" || branch.matches("branch-[\\d.]+".toRegex())) &&
                gradle.taskGraph.hasTask(":artifactoryPublish")
        }
        sign(publishing.publications)
    }

    tasks.withType<Sign> {
        onlyIf {
            val branch = System.getenv()["CIRRUS_BRANCH"] ?: ""
            val artifactorySkip: Boolean = tasks.artifactoryPublish.get().skip
            !artifactorySkip && (branch == "master" || branch.matches("branch-[\\d.]+".toRegex())) &&
                gradle.taskGraph.hasTask(":artifactoryPublish")
        }
    }
}

sonarqube {
    properties {
        property("sonar.links.ci", "https://cirrus-ci.com/github/SonarSource/sonar-kotlin")
        property("sonar.projectName", projectTitle)
        property("sonar.links.scm", "https://github.com/SonarSource/sonar-kotlin")
        property("sonar.links.issue", "https://jira.sonarsource.com/browse/SONARKT")
        property("sonar.exclusions", "**/build/**/*")
    }
}

subprojects {
    sonarqube.properties {
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            listOf(
                "build/reports/jacoco/test/jacocoTestReport.xml",
                "${project.rootDir}/sonar-kotlin-plugin/build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml"
            ).joinToString(",")
        )
    }
}

artifactory {
    clientConfig.info.buildName = "sonar-kotlin"
    clientConfig.info.buildNumber = System.getenv("BUILD_NUMBER")
    clientConfig.isIncludeEnvVars = true
    clientConfig.envVarsExcludePatterns =
        "*password*,*PASSWORD*,*secret*,*MAVEN_CMD_LINE_ARGS*,sun.java.command,*token*,*TOKEN*,*LOGIN*,*login*,*key*,*KEY*,*PASSPHRASE*,*signing*"

    // Define the artifacts to be deployed to https://binaries.sonarsource.com on releases
    clientConfig.info.addEnvironmentProperty("ARTIFACTS_TO_PUBLISH", "${project.group}:sonar-kotlin-plugin:jar")
    clientConfig.info.addEnvironmentProperty("ARTIFACTS_TO_DOWNLOAD", "")
    // The name of this variable is important because it"s used by the delivery process when extracting version from Artifactory build info.
    clientConfig.info.addEnvironmentProperty("PROJECT_VERSION", version.toString())

    setContextUrl(System.getenv("ARTIFACTORY_URL"))
    publish {
        repository {
            setRepoKey(System.getenv("ARTIFACTORY_DEPLOY_REPO"))
            setUsername(System.getenv("ARTIFACTORY_DEPLOY_USERNAME"))
            setPassword(System.getenv("ARTIFACTORY_DEPLOY_PASSWORD"))
        }

        defaults {
            setProperties(
                mapOf(
                    "build.name" to "sonar-kotlin",
                    "build.number" to System.getenv("BUILD_NUMBER"),
                    "pr.branch.target" to System.getenv("PULL_REQUEST_BRANCH_TARGET"),
                    "pr.number" to System.getenv("PULL_REQUEST_NUMBER"),
                    "vcs.branch" to System.getenv("GIT_BRANCH"),
                    "vcs.revision" to System.getenv("GIT_COMMIT"),
                    "version" to project.version as String,
                )
            )
            publications("mavenJava")
            setPublishPom(true) // Publish generated POM files to Artifactory (true by default)
            setPublishIvy(false) // Publish generated Ivy descriptor files to Artifactory (true by default)
        }
    }
}

tasks.register<CreateKotlinRuleStubsTask>("setupRuleStubs") {
    group = "Rules"
    description = "Generate required stubs for a new Kotlin rule"
    finalizedBy(tasks.findByPath(":generateRuleMetadata"))
}

tasks.register<CreateKotlinGradleRuleStubsTask>("setupGradleRuleStubs") {
    group = "Rules"
    description = "Generate required stubs for a new Kotlin Gradle DSL rule"
    finalizedBy(tasks.findByPath(":generateRuleMetadata"))
}

tasks.register<FetchRuleMetadata.FetchSpecificRulesMetadata>("generateRuleMetadata")
tasks.register<FetchRuleMetadata.FetchAllRulesMetadata>("updateRuleMetadata")
