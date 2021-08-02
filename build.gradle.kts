import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import java.util.Calendar
import java.util.jar.JarInputStream

plugins {
    java
    id("jacoco")
    id("com.github.hierynomus.license") version "0.16.1"
    id("com.jfrog.artifactory") version "4.24.14"
    id("io.spring.dependency-management") version "1.0.6.RELEASE" apply false
    id("org.sonarqube") version "3.3"
    id("de.thetaphi.forbiddenapis") version "3.0" apply false
    id("org.jetbrains.kotlin.jvm") apply false
    `maven-publish`
    signing
}

allprojects {
    apply<JavaPlugin>()
    apply(plugin = "jacoco")
    apply(plugin = "io.spring.dependency-management")
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

    ext {
        set("buildNumber", System.getProperty("buildNumber"))
        set("analyzerCommonsVersion", "1.14.1.690")
        set("sonarqubeVersion", "7.9")
        set("orchestratorVersion", "3.35.0.2707")
        set("sonarlintVersion", "4.2.0.2266")
        set("sonarLinksCi", "https://travis-ci.org/SonarSource/slang")

        set("artifactsToPublish", "")
        set("artifactsToDownload", "")
    }
    // Replaces the version defined in sources, usually x.y-SNAPSHOT, by a version identifying the build.
    if (project.version.toString().endsWith("-SNAPSHOT") && ext["buildNumber"] != null) {
        val versionSuffix =
            if (project.version.toString().count { it == '.' } == 1) ".0.${ext["buildNumber"]}" else ".${ext["buildNumber"]}"
        project.version = project.version.toString().replace("-SNAPSHOT", versionSuffix)
    }

    val extraProperties = File(rootDir, "private/extraProperties.gradle")
    if (extraProperties.exists()) {
        apply(from = extraProperties)
    }

    repositories {
        mavenLocal()
        val repository = if (project.hasProperty("qa")) "sonarsource-qa" else "sonarsource"
        maven {
            url = uri("https://repox.jfrog.io/repox/${repository}")
        }
    }
}

val projectTitle: String by project

subprojects {
    val javadoc: Javadoc by tasks

    // do not publish to Artifactory by default
    tasks.artifactoryPublish {
        skip = true
    }

    apply(plugin = "com.github.hierynomus.license")
    apply(plugin = "io.spring.dependency-management")

    configure<DependencyManagementExtension> {
        dependencies {
            dependency("org.sonarsource.sonarqube:sonar-plugin-api:${extra["sonarqubeVersion"]}")
            dependency("org.sonarsource.sonarqube:sonar-ws:${extra["sonarqubeVersion"]}")
            dependency("com.google.code.findbugs:jsr305:1.3.9")
            dependency("com.eclipsesource.minimal-json:minimal-json:0.9.5")
            dependency("org.junit.jupiter:junit-jupiter-api:5.7.1")
            dependency("org.junit.jupiter:junit-jupiter-engine:5.7.1")
            dependency("org.junit.jupiter:junit-jupiter-migrationsupport:5.7.1")
            dependency("junit:junit:4.13.1")
            dependency("org.mockito:mockito-core:2.21.0")
            dependency("org.assertj:assertj-core:3.6.1")
            dependency("io.github.classgraph:classgraph:4.8.90")
            dependency("org.sonarsource.analyzer-commons:sonar-analyzer-test-commons:${extra["analyzerCommonsVersion"]}")
            dependency("org.sonarsource.analyzer-commons:sonar-analyzer-commons:${extra["analyzerCommonsVersion"]}")
            dependency("org.sonarsource.analyzer-commons:sonar-xml-parsing:${extra["analyzerCommonsVersion"]}")
            dependency("org.sonarsource.orchestrator:sonar-orchestrator:${extra["orchestratorVersion"]}")
            dependency("org.sonarsource.sonarlint.core:sonarlint-core:${extra["sonarlintVersion"]}")
        }
    }

    java.sourceCompatibility = JavaVersion.VERSION_1_8
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(java.sourceCompatibility.majorVersion.toInt())
    }

    jacoco {
        toolVersion = "0.8.7"
    }

    tasks.jacocoTestReport {
        reports {
            xml.setEnabled(true)
            csv.setEnabled(false)
            html.setEnabled(false)
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
        // Prevent accidental use of JUnit 4, which is still present in dependencies due to junit-jupiter-migrationsupport
        apply(plugin = "de.thetaphi.forbiddenapis")

        tasks.named<de.thetaphi.forbiddenapis.gradle.CheckForbiddenApis>("forbiddenApisMain") {
            signatures = listOf("")
        }

        tasks.named<de.thetaphi.forbiddenapis.gradle.CheckForbiddenApis>("forbiddenApisTest") {
            signatures = listOf(
                "org.junit.Test @ use JUnit 5 org.junit.jupiter.api.Test instead",
                "org.junit.Before @ use JUnit 5 org.junit.jupiter.api.BeforeEach instead",
                "org.junit.After @ use JUnit 5 org.junit.jupiter.api.AfterEach instead",
                "org.junit.BeforeClass @ use JUnit 5 org.junit.jupiter.api.BeforeAll instead",
                "org.junit.AfterClass @ use JUnit 5 org.junit.jupiter.api.AfterAll instead",
                "org.junit.Ignore @ use JUnit 5 org.junit.jupiter.api.Disabled instead",
                "org.junit.Assert @ use JUnit 5 org.junit.jupiter.api.Assertions or org.assertj.core.api.Assertions instead",
                "org.junit.Assume @ use JUnit 5 org.junit.jupiter.api.Assumptions instead"
            )
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
        classifier = "sources"
        from(sourceSets.main.get().allSource)
    }

    val javadocJar by tasks.creating(Jar::class) {
        dependsOn(javadoc)
        classifier = "javadoc"
        from(tasks["javadoc"])
    }

    license {
        header = rootDir.absoluteFile.resolve("LICENSE_HEADER")
        ext["name"] = "SonarSource SLang"
        ext["inceptionYear"] = "2009" // TODO replace by 2021
        ext["year"] = Calendar.getInstance().get(Calendar.YEAR)
        isStrictCheck = true
        mapping(
            mapOf(
                "java" to "SLASHSTAR_STYLE",
                "js" to "SLASHSTAR_STYLE",
                "ts" to "SLASHSTAR_STYLE",
                "tsx" to "SLASHSTAR_STYLE",
                "css" to "SLASHSTAR_STYLE",
                "less" to "SLASHSTAR_STYLE"
                //"kt" to "SLASHSTAR_STYLE" // TODO: enable, add .kt to includes
            )
        )
        includes(listOf("**/src/**/*.java", "**/*.js", "**/*.ts", "**/*.tsx", "**/*.css"))
        excludes(
            listOf(
                "**/src/test/resources/**",
                "**/build/**",
                "its/sources/**",
                "its/plugin/projects/**",
                "private/its/sources/**",
                "private/its/plugin/projects/**"
            )
        )
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
                            name.set("GNU LPGL 3")
                            url.set("http://www.gnu.org/licenses/lgpl.txt")
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
        val sonarLinksCi: String by extra
        property("sonar.links.ci", sonarLinksCi)
        property("sonar.projectName", projectTitle)
        property("sonar.links.scm", "https://github.com/SonarSource/sonar-kotlin")
        property("sonar.links.issue", "https://jira.sonarsource.com/browse/SONARKT")
        property("sonar.exclusions", "**/build/**/*")
    }
}

artifactory {
    clientConfig.info.buildName = "sonar-kotlin"
    clientConfig.info.buildNumber = System.getenv("BUILD_NUMBER")
    clientConfig.isIncludeEnvVars = true
    clientConfig.envVarsExcludePatterns =
        "*password*,*PASSWORD*,*secret*,*MAVEN_CMD_LINE_ARGS*,sun.java.command,*token*,*TOKEN*,*LOGIN*,*login*,*key*,*KEY*,*PASSPHRASE*,*signing*"

    val artifactsToPublish: String by project
    val artifactsToDownload: String by project
    // Define the artifacts to be deployed to https://binaries.sonarsource.com on releases
    clientConfig.info.addEnvironmentProperty("ARTIFACTS_TO_PUBLISH", artifactsToPublish)
    clientConfig.info.addEnvironmentProperty("ARTIFACTS_TO_DOWNLOAD", artifactsToDownload)
    // The name of this variable is important because it"s used by the delivery process when extracting version from Artifactory build info.
    clientConfig.info.addEnvironmentProperty("PROJECT_VERSION", version.toString())

    setContextUrl(System.getenv("ARTIFACTORY_URL"))
    publish {
        repository {
            setRepoKey(System.getenv("ARTIFACTORY_DEPLOY_REPO"))
            setUsername(System.getenv("ARTIFACTORY_DEPLOY_USERNAME"))
            setPassword(System.getenv("ARTIFACTORY_DEPLOY_PASSWORD"))
        }

        defaults(delegateClosureOf<groovy.lang.GroovyObject> {
            setProperty(
                "properties", mapOf(
                    "build.name" to "sonar-kotlin",
                    "build.number" to System.getenv("BUILD_NUMBER"),
                    "pr.branch.target" to System.getenv("PULL_REQUEST_BRANCH_TARGET"),
                    "pr.number" to System.getenv("PULL_REQUEST_NUMBER"),
                    "vcs.branch" to System.getenv("GIT_BRANCH"),
                    "vcs.revision" to System.getenv("GIT_COMMIT"),
                    "version" to project.version as String,

                    "publishPom" to "true",
                    "publishIvy" to "false"
                )
            )
            invokeMethod("publications", "mavenJava")
            setProperty("publishPom", true) // Publish generated POM files to Artifactory (true by default)
            setProperty("publishIvy", false) // Publish generated Ivy descriptor files to Artifactory (true by default)
        })
    }
}
