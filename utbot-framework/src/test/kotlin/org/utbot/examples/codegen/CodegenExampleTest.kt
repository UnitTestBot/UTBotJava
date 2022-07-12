package org.utbot.examples.codegen

import org.utbot.examples.UtTestCaseChecker
import org.utbot.examples.mock.MockRandomExamples
import org.utbot.examples.withoutConcrete
import kotlin.reflect.full.functions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class CodegenExampleTest : UtTestCaseChecker(testClass = CodegenExample::class) {
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