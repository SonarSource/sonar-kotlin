package org.sonarsource.kotlin

import io.mockk.InternalPlatformDsl.toStr
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.kotlin.verifier.DEFAULT_KOTLIN_CLASSPATH
import org.sonarsource.kotlin.verifier.KotlinVerifier
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class IncorrectResolutionTest {
    @Test
    fun `demo incorrect kotlin resolution - should throw exception`() {

        /*
         * If we can't find the `kotlin.collections.toString(Charset)` function (as we do here by filtering it from the classpath and deps),
         * then the Kotlin compiler resolves a call to said method to the regular `kotlin.ByteArray.toString()` function instead.
         * Is this a compiler bug?
         */

        KotlinVerifier(DemoIncorrectResolutionCheck()) {
            this.fileName = "IncorrectResolutionSample.kt"
            this.classpath = (System.getProperty("java.class.path").split(System.getProperty("path.separator")) + DEFAULT_KOTLIN_CLASSPATH)
                .filter { !it.contains("stdlib") }
            this.deps = getClassPath(DEFAULT_TEST_JARS_DIRECTORY)
                .filter { !it.contains("stdlib") }
            this.isAndroid = false
        }.verifyNoIssue()
    }
}


class DemoIncorrectResolutionCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(FunMatcher(qualifier = "kotlin.ByteArray") {
        withNames("toString")
        withNoArguments()
    })

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        // We should not enter this, as we are looking for the no-arg 'toString' defined in `kotlin.ByteArray` but in the sample file
        // we only call `kotlin.collections.toString(Charset)`.
        println("Incorrectly resolved!")
        throw IllegalStateException("Incorrectly resolved!")
    }
}

// Below here is copy + pasted from KotlinVerifier

private val DEFAULT_TEST_JARS_DIRECTORY = "../kotlin-checks-test-sources/build/test-jars"

private fun getClassPath(jarsDirectory: String): List<String> {
    var classpath = mutableListOf<String>()
    val testJars = Paths.get(jarsDirectory)
    if (testJars.toFile().exists()) {
        classpath = getFilesRecursively(testJars)
    } else if (DEFAULT_TEST_JARS_DIRECTORY != jarsDirectory) {
        throw AssertionError(
            "The directory to be used to extend class path does not exists (${testJars.toAbsolutePath()})."
        )
    }
    return classpath
}

private fun getFilesRecursively(root: Path): MutableList<String> {
    val files: MutableList<String> = ArrayList()
    val visitor: FileVisitor<Path> = object : SimpleFileVisitor<Path>() {
        override fun visitFile(filePath: Path, attrs: BasicFileAttributes): FileVisitResult {
            files.add(filePath.toStr())
            return FileVisitResult.CONTINUE
        }

        override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
            return FileVisitResult.CONTINUE
        }
    }
    Files.walkFileTree(root, visitor)
    return files
}
