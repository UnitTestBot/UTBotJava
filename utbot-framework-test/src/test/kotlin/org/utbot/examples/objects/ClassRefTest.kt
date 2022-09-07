@file:Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")

package org.utbot.examples.objects

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.atLeast
import org.utbot.framework.plugin.api.CodegenLanguage
import java.lang.Boolean
import kotlin.Array
import kotlin.Suppress
import kotlin.arrayOf
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

internal class ClassRefTest : UtValueTestCaseChecker(
    testClass = ClassRef::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        // TODO: SAT-1457 Restore Kotlin codegen for a group of tests with type casts
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    fun testTakeBooleanClassRef() {
        check(
            ClassRef::takeBooleanClassRef,
            eq(1),
            { r -> r == Boolean.TYPE }
        )
    }

    @Test
    fun testTakeClassRef() {
        check(
            ClassRef::takeClassRef,
            eq(1),
            { r -> r == ClassRef::class.java }
        )
    }

    @Test
    fun testTakeClassRefFromParam() {
        check(
            ClassRef::takeClassRefFromParam,
            eq(2),
            { classRef, _ -> classRef == null },
            { classRef, r -> r == classRef.javaClass }
        )
    }


    @Test
    fun testTakeArrayClassRef() {
        check(
            ClassRef::takeArrayClassRef,
            eq(1),
            { r -> r == arrayOf<ClassRef>()::class.java }
        )
    }

    @Test
    fun testTwoDimArrayClassRef() {
        check(
            ClassRef::twoDimArrayClassRef,
            eq(1),
            { r -> r == arrayOf<Array<ClassRef>>()::class.java }
        )
    }

    @Test
    fun testTwoDimArrayClassRefFromParam() {
        check(
            ClassRef::twoDimArrayClassRefFromParam,
            eq(2),
            { array, _ -> array == null },
            { array, r -> r == array::class.java }
        )
    }

    @Test
    fun testTakeConstantClassRef() {
        check(
            ClassRef::takeConstantClassRef,
            eq(1),
            { r -> r == ClassRef::class.java }
        )
    }

    @Test
    fun testEqualityOnClassRef() {
        check(
            ClassRef::equalityOnClassRef,
            eq(1),
            { r -> r == true },
            coverage = atLeast(50) // we cannot find a way to have different class references
        )
    }

    @Test
    fun testEqualityOnStringClassRef() {
        check(
            ClassRef::equalityOnStringClassRef,
            eq(1),
            { r -> r == true },
            coverage = atLeast(50) // we cannot find a way to have different class references
        )
    }

    @Test
    fun testEqualityOnArrayClassRef() {
        check(
            ClassRef::equalityOnArrayClassRef,
            eq(1),
            { r -> r == true },
            coverage = atLeast(50) // we cannot find a way to have different class references
        )
    }

    @Test
    fun testTwoDimensionalArrayClassRef() {
        check(
            ClassRef::twoDimensionalArrayClassRef,
            eq(1),
            { r -> r == true },
            coverage = atLeast(50)
        )
    }

    @Test
    fun testEqualityOnGenericClassRef() {
        check(
            ClassRef::equalityOnGenericClassRef,
            eq(1),
            { r -> r == true },
            coverage = atLeast(50) // we cannot find a way to have different class references
        )
    }
}