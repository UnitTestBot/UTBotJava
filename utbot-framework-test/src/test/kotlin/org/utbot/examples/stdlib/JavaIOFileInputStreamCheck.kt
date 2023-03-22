package org.utbot.examples.stdlib

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.testcheckers.eq
import org.utbot.testing.CodeGeneration
import org.utbot.testing.DoNotCalculate
import org.utbot.testing.UtValueTestCaseChecker

internal class JavaIOFileInputStreamCheckTest : UtValueTestCaseChecker(
    testClass = JavaIOFileInputStreamCheck::class,
    testCodeGeneration = true,
    pipelines = listOf(
        TestLastStage(CodegenLanguage.JAVA),
        TestLastStage(CodegenLanguage.KOTLIN, CodeGeneration) // because of mocks
    )) {
    @Test
    fun testRead() {
            checkMocksAndInstrumentation(
                JavaIOFileInputStreamCheck::read,
                eq(1),
                { _, _, instrumentation, r ->
                    val constructorMock = instrumentation.single() as UtNewInstanceInstrumentation

                    val classIdEquality = constructorMock.classId == java.io.FileInputStream::class.id
                    val callSiteIdEquality = constructorMock.callSites.single() == JavaIOFileInputStreamCheck::class.id
                    val instance = constructorMock.instances.single() as UtCompositeModel
                    val methodMock = instance.mocks.entries.single()
                    val methodNameEquality = methodMock.key.name == "read"
                    val mockValueResult = r == (methodMock.value.single() as UtPrimitiveModel).value as Int

                    classIdEquality && callSiteIdEquality && instance.isMock && methodNameEquality && mockValueResult
                },
                additionalMockAlwaysClasses = setOf(java.io.FileInputStream::class.id),
                coverage = DoNotCalculate // there is a problem with coverage calculation of mocked values
            )
    }
}