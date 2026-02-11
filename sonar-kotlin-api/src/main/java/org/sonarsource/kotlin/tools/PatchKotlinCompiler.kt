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
package org.sonarsource.kotlin.tools

import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.io.File

fun main() {
    patchAppScheduledExecutorService()
    patchKtVisitor()
}

/**
 * Patches [org.jetbrains.kotlin.psi.KtVisitor]
 * to get rid of folded string literals - see https://youtrack.jetbrains.com/issue/KT-78843
 *
 * TODO check whether the [fix](https://github.com/JetBrains/kotlin/commit/1118af763f6e6405c9f0668582d7dee153a72e8)
 * for the above issue is appropriate for us and sonar-security and can supersede this patch
 */
private fun patchKtVisitor() {
    val cls = "org/jetbrains/kotlin/psi/KtVisitor.class"
    val bytes = object {}.javaClass
        .getResourceAsStream("/$cls")!!
        .use { it.readAllBytes() }
    val classWriter = ClassWriter(0)

    val classNode = ClassNode()
    ClassReader(bytes).accept(classNode, 0)
    val methodNode = classNode.methods.find { it.name == "visitBinaryExpression" }!!
    methodNode.instructions.clear()
    methodNode.visitVarInsn(Opcodes.ALOAD, 0)
    methodNode.visitVarInsn(Opcodes.ALOAD, 1)
    methodNode.visitVarInsn(Opcodes.ALOAD, 2)
    methodNode.visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        "org/jetbrains/kotlin/psi/KtVisitor",
        "visitExpression",
        "(Lorg/jetbrains/kotlin/psi/KtExpression;Ljava/lang/Object;)Ljava/lang/Object;",
        false
    )
    methodNode.visitInsn(Opcodes.ARETURN)
    classNode.accept(classWriter)

    val output = File("build/patch").resolve(cls)
    output.parentFile.mkdirs()
    output.writeBytes(classWriter.toByteArray())
}

/**
 * Patches [com.intellij.util.concurrency.AppScheduledExecutorService.MyThreadFactory].
 */
private fun patchAppScheduledExecutorService() {

    var patched = false

    class MyMethodVisitor(mv: MethodVisitor) : MethodVisitor(Opcodes.API_VERSION, mv) {
        override fun visitMethodInsn(
            opcode: Int,
            owner: String?,
            name: String?,
            descriptor: String?,
            isInterface: Boolean
        ) {
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
            if (name == "newThread") { // INVOKEINTERFACE java/util/concurrent/ThreadFactory.newThread (Ljava/lang/Runnable;)Ljava/lang/Thread; (itf)
                mv.visitInsn(Opcodes.DUP)
                mv.visitInsn(Opcodes.ICONST_1)
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Thread", "setDaemon", "(Z)V", false)
                patched = true
            }
        }
    }

    class MyClassVisitor(cv: ClassVisitor) : ClassVisitor(Opcodes.ASM9, cv) {
        override fun visitMethod(
            access: Int,
            name: String?,
            descriptor: String?,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor {
            return MyMethodVisitor(super.visitMethod(access, name, descriptor, signature, exceptions))
        }
    }

    val cls = "com/intellij/util/concurrency/AppScheduledExecutorService\$MyThreadFactory.class"
    val bytes = object {}.javaClass
        .getResourceAsStream("/$cls")!!
        .use { it.readAllBytes() }
    val classWriter = ClassWriter(0)
    ClassReader(bytes).accept(MyClassVisitor(classWriter), 0)

    if (!patched) throw AssertionError()

    val output = File("build/patch").resolve(cls)
    output.parentFile.mkdirs()
    output.writeBytes(classWriter.toByteArray())
}
