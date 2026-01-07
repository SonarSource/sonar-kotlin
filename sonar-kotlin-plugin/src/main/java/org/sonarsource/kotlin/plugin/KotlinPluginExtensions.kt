/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.plugin

import org.sonar.api.rule.RuleKey
import org.sonar.api.utils.AnnotationUtils
import org.sonar.check.Rule
import com.sonarsource.plugins.kotlin.api.KotlinPluginExtensionsProvider
import java.net.URL

/**
 * Loads extensions so that classes within packages (and subpackages) of [KotlinPluginExtensionsProvider] implementations
 * can use all classes of the plugin even if they are outside
 * [predefined packages](https://github.com/SonarSource/sonarqube/blob/10.7.0.96327/sonar-core/src/main/java/org/sonar/core/platform/PluginClassLoader.java#L51).
 */
class KotlinPluginExtensions(
    providers: Array<KotlinPluginExtensionsProvider>,
) {

    private val classLoader: ExtensionsClassLoader
    private val repositories: MutableMap<String, String> = mutableMapOf()
    private val rulesByRepositoryKey: MutableMap<String, MutableList<Class<*>>> = mutableMapOf()
    private val rulesEnabledInSonarWayProfile: MutableList<RuleKey> = mutableListOf()

    init {
        val classLoaders: MutableMap<String, ClassLoader> = mutableMapOf();
        providers.forEach {
            val providerClass = it.javaClass
            classLoaders[providerClass.packageName + "."] = providerClass.classLoader
        }
        classLoaders["org.sonarsource.analyzer.commons."] = javaClass.classLoader
        classLoader = ExtensionsClassLoader(javaClass.classLoader, classLoaders)

        providers.forEach {
            val providerClass = it.javaClass
            @Suppress("UNCHECKED_CAST")
            val provider = (classLoader.defineClass(providerClass.classLoader, providerClass.name) as Class<KotlinPluginExtensionsProvider>)
                .getConstructor().newInstance()
            provider.registerKotlinPluginExtensions(object : KotlinPluginExtensionsProvider.Extensions {
                override fun registerRepository(repositoryKey: String, name: String) {
                    repositories[repositoryKey] = name
                }

                override fun registerRule(repositoryKey: String, ruleClass: Class<*>, enabledInSonarWay: Boolean) {
                    val ruleKey = AnnotationUtils.getAnnotation(ruleClass, Rule::class.java).key
                    rulesByRepositoryKey.computeIfAbsent(repositoryKey) { mutableListOf() }.add(ruleClass)
                    if (enabledInSonarWay) {
                        rulesEnabledInSonarWayProfile.add(RuleKey.of(repositoryKey, ruleKey))
                    }
                }
            })
        }
    }

    fun repositories(): Map<String, String> = repositories
    fun rulesByRepositoryKey(): Map<String, List<Class<*>>> = rulesByRepositoryKey
    fun rulesEnabledInSonarWayProfile(): List<RuleKey> = rulesEnabledInSonarWayProfile

    /**
     * @return [org.sonarsource.analyzer.commons.RuleMetadataLoader] class
     * which is able to load [org.sonarsource.analyzer.commons.Resources] from other SonarQube plugins
     */
    fun ruleMetadataLoaderClass(): Class<*> {
        return classLoader.loadClass("org.sonarsource.analyzer.commons.RuleMetadataLoader")
    }

}

private class ExtensionsClassLoader(
    parentClassLoader: ClassLoader,
    private val classLoaders: Map<String, ClassLoader>,
) : ClassLoader(parentClassLoader) {

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        val loadedClass = findLoadedClass(name)
        if (loadedClass != null) {
            return loadedClass
        }
        for ((prefix, classLoader) in classLoaders.entries) {
            if (name.startsWith(prefix)) {
                val aClass = defineClass(classLoader, name)
                if (resolve) {
                    resolveClass(aClass)
                }
                return aClass
            }
        }
        val aClass = try {
            parent.loadClass(name)
        } catch (ignore: ClassNotFoundException) {
            findClass(name)
        }
        if (resolve) {
            resolveClass(aClass)
        }
        return aClass
    }

    @Throws(ClassNotFoundException::class)
    fun defineClass(classLoader: ClassLoader, name: String): Class<*> {
        val classBytes = classLoader.getResourceAsStream(name.replace('.', '/') + ".class")
            ?.use { it.readAllBytes() }
            ?: throw ClassNotFoundException(name)
        return defineClass(name, classBytes, 0, classBytes.size)
    }

    @Throws(ClassNotFoundException::class)
    override fun findClass(name: String?): Class<*> {
        for (classLoader in classLoaders.values) {
            try {
                return classLoader.loadClass(name)
            } catch (ignore: ClassNotFoundException) {
                // ignored
            }
        }
        throw ClassNotFoundException(name)
    }

    override fun findResource(name: String?): URL? {
        for (classLoader in classLoaders.values) {
            val url = classLoader.getResource(name)
            if (url != null) {
                return url
            }
        }
        return null
    }

}
