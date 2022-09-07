package org.utbot.examples.codegen

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.examples.mock.MockRandomExamples
import kotlin.reflect.full.functions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.withoutConcrete

internal class CodegenExampleTest : UtValueTestCaseChecker(testClass = CodegenExample::class) {
    @Test
    fun firstExampleTest() {
        withoutConcrete {
            checkAllCombinations(
                CodegenExample::firstExample,
            )
        }
    }

    @Test
    fun innerClassTest() {
        val innerClass = CodegenExample::class.nestedClasses.single { it.simpleName == "InnerClass" }
        val fooMethod = innerClass.functions.single { it.name == "foo" }

        checkAllCombinations(fooMethod)
    }

    @Test
    @Disabled("TODO static initializers JIRA:1483")
    fun staticClassTest() {
        val staticClass = CodegenExample::class.nestedClasses.single { it.simpleName == "StaticClass" }
        val barMethod = staticClass.functions.single { it.name == "bar" }

        checkAllCombinations(barMethod)
    }

    @Test
    fun randomAsLocalVariableTest() {
        checkAllCombinations(
            MockRandomExamples::randomAsLocalVariable,
        )
    }

    @Test
    fun randomAsFieldTest() {
        checkAllCombinations(
            MockRandomExamples::randomAsField,
        )
    }

    @Test
    fun randomAsParameterTest() {
        checkAllCombinations(
            MockRandomExamples::randomAsParameter,
        )
    }
}