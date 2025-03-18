/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
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
import java.io.File

/**
 * Patches [com.intellij.util.concurrency.AppScheduledExecutorService.MyThreadFactory].
 */
fun main() {

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
    File("build/patch/test.txt").writeText("test")
    output.writeBytes(classWriter.toByteArray())
}
