package org.utbot.examples.objects

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.isException
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutConcrete
import org.utbot.tests.infrastructure.CodeGeneration
import org.utbot.tests.infrastructure.Compilation

// TODO Kotlin compilation SAT-1332
// Code generation executions fail due we cannot analyze strings properly for now
internal class ClassWithClassRefTest : UtValueTestCaseChecker(
    testClass = ClassWithClassRef::class,
    testCodeGeneration = true,
    languagePipelines = listOf(
        CodeGenerationLanguageLastStage(CodegenLanguage.JAVA, Compilation), // TODO JIRA:1479
        CodeGenerationLanguageLastStage(CodegenLanguage.KOTLIN, CodeGeneration)
    )
) {
    @Test
    // TODO test does not work properly JIRA:1479
    // TODO we don't fail now, but we do not generate correct value as well
    fun testClassRefGetName() {
        withoutConcrete { // TODO: concrete execution returns "java.lang.Object"
            checkWithThisAndException(
                ClassWithClassRef::classRefName,
                eq(2),
                { instance, r -> instance.someListClass == null && r.isException<NullPointerException>() },
                { instance, r -> instance.someListClass != null && r.getOrNull() == "" },
                coverage = DoNotCalculate // TODO: Method coverage with `this` parameter isn't supported
            )
        }
    }
}