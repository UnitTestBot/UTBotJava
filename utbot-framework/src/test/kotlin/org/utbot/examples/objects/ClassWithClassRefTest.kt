package org.utbot.examples.objects

import org.utbot.examples.UtValueTestCaseChecker
import org.utbot.examples.DoNotCalculate
import org.utbot.examples.eq
import org.utbot.examples.isException
import org.utbot.examples.withoutConcrete
import org.utbot.framework.codegen.CodeGeneration
import org.utbot.framework.codegen.Compilation
import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test

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