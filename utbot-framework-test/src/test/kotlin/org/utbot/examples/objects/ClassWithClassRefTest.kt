package org.utbot.examples.objects

import org.utbot.framework.plugin.api.CodegenLanguage
import org.junit.jupiter.api.Test
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.testcheckers.eq
import org.utbot.testcheckers.withoutConcrete
import org.utbot.testing.*

// TODO Kotlin compilation SAT-1332
// Code generation executions fail due we cannot analyze strings properly for now
internal class ClassWithClassRefTest : UtValueTestCaseChecker(
    testClass = ClassWithClassRef::class,
    testCodeGeneration = true,
    // TODO JIRA:1479
    configurations = listOf(
        Configuration(CodegenLanguage.JAVA, ParametrizedTestSource.DO_NOT_PARAMETRIZE, Compilation),
        Configuration(CodegenLanguage.JAVA, ParametrizedTestSource.PARAMETRIZE, Compilation),
        Configuration(CodegenLanguage.KOTLIN, ParametrizedTestSource.DO_NOT_PARAMETRIZE, CodeGeneration),
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