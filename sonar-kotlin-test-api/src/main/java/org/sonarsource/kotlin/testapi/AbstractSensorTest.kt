/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.testapi

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.rule.internal.ActiveRulesBuilder
import org.sonar.api.batch.rule.internal.NewActiveRule
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.measures.FileLinesContext
import org.sonar.api.measures.FileLinesContextFactory
import org.sonar.api.rule.RuleKey
import org.sonar.api.testfixtures.log.LogTesterJUnit5
// TODO: testapi should not depend on api module.
import org.sonarsource.kotlin.api.common.KOTLIN_REPOSITORY_KEY
import org.sonarsource.kotlin.api.common.KotlinLanguage
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

abstract class AbstractSensorTest {

    @JvmField
    @TempDir
    var temp: Path? = null

    protected lateinit var baseDir: Path
    protected lateinit var context: SensorContextTester
    protected var fileLinesContextFactory: FileLinesContextFactory = Mockito.mock(FileLinesContextFactory::class.java)

    @JvmField
    @RegisterExtension
    var logTester = LogTesterJUnit5()

    @BeforeEach
    fun setup() {
        baseDir = createTempDirectory(temp!!)
        context = SensorContextTester.create(baseDir.toRealPath())
        val fileLinesContext = Mockito.mock(FileLinesContext::class.java)
        Mockito.`when`(
            fileLinesContextFactory.createFor(
                ArgumentMatchers.any(
                    InputFile::class.java
                )
            )
        ).thenReturn(fileLinesContext)
    }

    protected fun checkFactory(vararg ruleKeys: String): CheckFactory {
        return checkFactory(ruleKeys.map { RuleKey.of(KOTLIN_REPOSITORY_KEY, it) })
    }

    protected fun checkFactory(ruleKeys: List<RuleKey>): CheckFactory {
        val builder = ActiveRulesBuilder()
        for (ruleKey in ruleKeys) {
            val newRule = NewActiveRule.Builder()
                .setRuleKey(ruleKey)
                .setName(ruleKey.rule())
                .build()
            builder.addRule(newRule)
        }
        context.setActiveRules(builder.build())
        return CheckFactory(context.activeRules())
    }

    protected fun createInputFile(relativePath: String, content: String, status: InputFile.Status=InputFile.Status.SAME): InputFile {
        return TestInputFileBuilder("moduleKey", relativePath)
            .setModuleBaseDir(baseDir)
            .setType(InputFile.Type.MAIN)
            .setLanguage(language().key)
            .setCharset(StandardCharsets.UTF_8)
            .setContents(content)
            .setStatus(status)
            .build()
    }

    // TODO: testapi should not depend on API (it will make frontend module tests depend on api module)
    fun language(): KotlinLanguage = KotlinLanguage(MapSettings().asConfig())
}
