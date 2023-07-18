package org.utbot.examples.mock.fields

import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.testcheckers.eq
import org.utbot.testing.UtValueTestCaseChecker

class ClassUsingClassWithRandomFieldTest : UtValueTestCaseChecker(
    testClass = ClassUsingClassWithRandomField::class,
    testCodeGeneration = true,
    // because of mocks
    configurations = ignoreKotlinCompilationConfigurations,
) {
    @Test
    fun testUseClassWithRandomField() {
        checkMocksAndInstrumentation(
            ClassUsingClassWithRandomField::useClassWithRandomField,
            eq(1),
            { mocks, instrumentation, r ->
                val noMocks = mocks.isEmpty()

                val constructorMock = instrumentation.single() as UtNewInstanceInstrumentation
                val classIdEquality = constructorMock.classId == java.util.Random::class.id
                val callSiteIdEquality = constructorMock.callSites.single() == ClassWithRandomField::class.id
                val instance = constructorMock.instances.single() as UtCompositeModel
                val methodMock = instance.mocks.entries.single()
                val methodNameEquality = methodMock.key.name == "nextInt"
                val mockValueResult = r == (methodMock.value.single() as UtPrimitiveModel).value as Int

                noMocks && classIdEquality && callSiteIdEquality && instance.isMock && methodNameEquality && mockValueResult
            }
        )
    }
}
