package org.utbot.instrumentation.instrumentation.instrumenter.visitors.util

import org.objectweb.asm.MethodVisitor

interface FieldInitializer {
    val name: String

    val descriptor: String

    val signature: String? // used for generics

    fun initField(mv: MethodVisitor): MethodVisitor
}

interface StaticFieldInitializer : FieldInitializer

interface InstanceFieldInitializer : FieldInitializer