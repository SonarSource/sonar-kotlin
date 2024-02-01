/**
 * Authenticate repox.jfrog.io repositories with Bearer scheme
 *
 * Credentials can be set by using one of these options:
 * - property:
 *   - ~/.gradle/gradle.properties:
 *     - {repo name}AuthHeaderValue=...
 *     - {repo name}AuthAccessToken=...
 *     - artifactoryPassword=...
 *   - cli arg:
 *     - -P{repo name}AuthHeaderValue=...
 *     - -P{repo name}AuthAccessToken=...
 *     - -PartifactoryPassword=...
 *     - -Dorg.gradle.project.{repo name}AuthHeaderValue=...
 *     - -Dorg.gradle.project.{repo name}AuthAccessToken=...
 *     - -Dorg.gradle.project.artifactoryPassword=...
 * - env var:
 *   - ORG_GRADLE_PROJECT_{repo name}AuthHeaderValue=...
 *   - ORG_GRADLE_PROJECT_{repo name}AuthAccessToken=...
 *   - ORG_GRADLE_PROJECT_artifactoryPassword=...
 *   - ARTIFACTORY_ACCESS_TOKEN=...
 *   - ARTIFACTORY_PASSWORD=...
 *   - ARTIFACTORY_PRIVATE_READER_TOKEN=...
 *   - ARTIFACTORY_PRIVATE_PASSWORD=...
 *   - ARTIFACTORY_DEPLOY_PASSWORD=...
 *
 * The first one presented will be used.
 */

beforeSettings {
    pluginManagement {
        // hook between repository configuration and plugin resolution
        resolutionStrategy {
            eachPlugin {
                repositories {
                    enableBearerAuthForRepoxRepositories(providers)
                }
            }
        }
    }
}

allprojects {
    beforeEvaluate {
        repositories {
            enableBearerAuthForRepoxRepositories(providers)
        }
    }
    afterEvaluate {
        repositories {
            enableBearerAuthForRepoxRepositories(providers)
        }
    }
}

class RepoxAuth {
    companion object {
        const val host = "repox.jfrog.io"
        const val authType = "header"
        const val authHeaderName = "Authorization"
        const val authValueScheme = "Bearer"
        val accessTokenEnvVars = listOf(
            "ARTIFACTORY_ACCESS_TOKEN",
            "ARTIFACTORY_PASSWORD",
            "ARTIFACTORY_PRIVATE_READER_TOKEN",
            "ARTIFACTORY_PRIVATE_PASSWORD",
            "ARTIFACTORY_DEPLOY_PASSWORD",
        )
    }
}

fun RepositoryHandler.addBearerAuthForRepoxRepositories(token: (String) -> Provider<String>) {
    filter {
        (it as? UrlArtifactRepository)?.url?.host == RepoxAuth.host
    }.forEach { repoCandidate ->
        (repoCandidate as? AuthenticationSupported)?.runCatching {
            if (authentication.any { it is HttpHeaderAuthentication }) return@forEach
            apply {
                credentials(HttpHeaderCredentials::class) {
                    name = RepoxAuth.authHeaderName
                    value = token(repoCandidate.name).map {
                        "${RepoxAuth.authValueScheme} ${it.substringAfter(" ")}"
                    }.orNull
                }
            }
        }?.onSuccess {
            it.authentication {
                add(create<HttpHeaderAuthentication>(RepoxAuth.authType))
            }
            logger.info(
                "Set '{}' auth for '{}' repository",
                RepoxAuth.authType,
                repoCandidate.name,
            )

        }
    }
}

fun <T> Provider<T>.orElse(vararg providers: Provider<T>) =
    listOf(this, *providers).reduce { p1, p2 ->
        p1.orElse(p2)
    }

fun RepositoryHandler.enableBearerAuthForRepoxRepositories(providers: ProviderFactory) {
    addBearerAuthForRepoxRepositories {
        providers.gradleProperty("${it}AuthHeaderValue").orElse(
            providers.gradleProperty("${it}AuthAccessToken"),
            providers.gradleProperty("artifactoryPassword"),
            *(RepoxAuth.accessTokenEnvVars.map { envVar ->
                providers.environmentVariable(envVar)
            }.toTypedArray())
        )
    }
}