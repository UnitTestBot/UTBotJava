package org.utbot.examples.collections

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.tests.infrastructure.CodeGeneration

// TODO disabled tests should be fixes with SAT-1441
internal class GenericListsExampleTest : UtValueTestCaseChecker(
    testClass = GenericListsExample::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA),
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    @Disabled("Doesn't find branches without NPE")
    fun testListOfListsOfT() {
        check(
            GenericListsExample<Long>::listOfListsOfT,
            eq(-1)
        )
    }

    @Test
    @Disabled("Problems with memory")
    fun testListOfComparable() {
        check(
            GenericListsExample<Long>::listOfComparable,
            eq(1),
            { v, r -> v != null && v.size > 1 && v[0] != null && v.all { it is Comparable<*> || it == null } && v == r },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun testListOfT() {
        check(
            GenericListsExample<Long>::listOfT,
            eq(1),
            { v, r -> v != null && v.size >= 2 && v[0] != null && v == r },
            coverage = DoNotCalculate
        )
    }

    @Test
    @Disabled("Wrong number of matchers")
    fun testListOfTArray() {
        check(
            GenericListsExample<Long>::listOfTArray,
            eq(1)
        )
    }

    @Test
    @Disabled("JIRA:1446")
    fun testListOfExtendsTArray() {
        check(
            GenericListsExample<Long>::listOfExtendsTArray,
            eq(-1)
        )
    }

    @Test
    @Disabled("java.lang.ClassCastException: java.util.ArraysParallelSortHelpers\$FJShort\$Merger cannot be cast to [I")
    fun testListOfPrimitiveArrayInheritors() {
        check(
            GenericListsExample<Long>::listOfPrimitiveArrayInheritors,
            eq(-1)
        )
    }

    @Test
    @Disabled("JIRA:1620")
    fun createWildcard() {
        check(
            GenericListsExample<*>::wildcard,
            eq(4),
            { v, r -> v == null && r?.isEmpty() == true },
            { v, r -> v != null && v.size == 1 && v[0] != null && v == r && v.all { it is Number || it == null } },
            { v, r -> v != null && (v.size != 1 || v[0] == null) && v == r && v.all { it is Number || it == null } },
            coverage = DoNotCalculate
        )
    }

    @Suppress("NestedLambdaShadowedImplicitParameter")
    @Test
    @Disabled("unexpected empty nested list")
    fun createListOfLists() {
        check(
            GenericListsExample<*>::listOfLists,
            eq(1),
            { v, r ->
                val valueCondition =  v != null && v[0] != null && v[0].isNotEmpty()
                val typeCondition = v.all { (it is List<*> && it.all { it is Int || it == null }) || it == null }

                valueCondition && typeCondition && v == r
            },
            coverage = DoNotCalculate
        )
    }

    @Test
    fun createWildcardWithOnlyQuestionMark() {
        check(
            GenericListsExample<*>::wildcardWithOnlyQuestionMark,
            eq(3),
            { v, r -> v == null && r?.isEmpty() == true },
            { v, r -> v.size == 1 && v == r },
            { v, r -> v.size != 1 && v == r },
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testGenericWithArrayOfPrimitives() {
        check(
            GenericListsExample<*>::genericWithArrayOfPrimitives,
            eq(1),
            { v, _ ->
                val valueCondition = v != null && v.size >= 2 && v[0] != null && v[0].isNotEmpty() && v[0][0] != 0L
                val typeCondition = v.all { it is LongArray || it == null }

                valueCondition && typeCondition
            },
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testGenericWithObject() {
        check(
            GenericListsExample<*>::genericWithObject,
            eq(1),
            { v, r -> v != null && v.size >= 2 && v[0] != null && v[0] is Long && v == r },
            coverage = DoNotCalculate
        )
    }


    @Test
    fun testGenericWithArrayOfArrays() {
        check(
            GenericListsExample<*>::genericWithArrayOfArrays,
            eq(1),
            { v, _ ->
                val valueCondition = v != null && v.size >= 2 && v[0] != null && v[0].isNotEmpty() && v[0][0] != null
                val typeCondition = v.all {
                    (it is Array<*> && it.isArrayOf<Array<*>>() && it.all { it.isArrayOf<Long>() || it == null}) || it == null
                }

                valueCondition && typeCondition
            },
            coverage = DoNotCalculate
        )
    }
}