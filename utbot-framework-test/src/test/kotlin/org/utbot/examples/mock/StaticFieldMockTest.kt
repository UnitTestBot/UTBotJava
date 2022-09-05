package org.utbot.examples.mock

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.examples.mock.provider.Provider
import org.utbot.examples.mock.service.impl.ExampleClass
import org.utbot.examples.mock.service.impl.ServiceWithStaticField
import org.utbot.tests.infrastructure.mocksMethod
import org.utbot.tests.infrastructure.value
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_PACKAGES
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class StaticFieldMockTest : UtValueTestCaseChecker(testClass = ServiceWithStaticField::class) {

    @Test
    fun testMockForStaticField_callMultipleMethods() {
        checkMocks(
            ServiceWithStaticField::callMultipleMethods,
            eq(3),
            { _, r -> r == null },
            { mocks, _ ->
                val firstMockConstraint = mocks[0].mocksMethod(Provider::provideInteger)
                val secondMockConstraint = mocks[1].mocksMethod(Provider::provideLong)
                val resultConstraint = mocks[0].value<Int>() < mocks[1].value<Long>()

                firstMockConstraint && secondMockConstraint && resultConstraint
            },
            { mocks, _ ->
                val firstMockConstraint = mocks[0].mocksMethod(Provider::provideInteger)
                val secondMockConstraint = mocks[1].mocksMethod(Provider::provideLong)
                val resultConstraint = mocks[0].value<Int>() >= mocks[1].value<Long>()

                firstMockConstraint && secondMockConstraint && resultConstraint
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_IntArgument() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnIntArgument,
            eq(3),
            { _, _, r -> r == null },
            { _, mocks, _ ->
                mocks.single().run {
                    mocksMethod(Provider::provideGiven) && value<Int>(0) < value<Int>(1)
                }
            },
            { _, mocks, _ ->
                mocks.single().run {
                    mocksMethod(Provider::provideGiven) && value<Int>(0) >= value<Int>(1)
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_BooleanPrimitive() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnBoolean,
            eq(3),
            { _, r -> r == null },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideBoolean) && value() } },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideBoolean) && !value<Boolean>() } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_inconsistentBoolean() {
        checkMocks(
            ServiceWithStaticField::inconsistentBoolean,
            eq(4),
            { _, r -> r == null },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideBoolean) && !value<Boolean>() } },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideBoolean) && value(0) && value(1) } },
            { mocks, _ ->
                mocks.single().run { mocksMethod(Provider::provideBoolean) && value(0) && !value<Boolean>(1) }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }


    @Test
    fun testMockForStaticField_CharacterPrimitive() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnCharacter,
            eq(3),
            { _, r -> r == null },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideCharacter) && value<Char>() > 'a' } },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideCharacter) && value<Char>() <= 'a' } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_BytePrimitive() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnByte,
            eq(3),
            { _, r -> r == null },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideByte) && value<Byte>() > 5 } },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideByte) && value<Byte>() <= 5 } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_ShortPrimitive() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnShort,
            eq(3),
            { _, r -> r == null },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideShort) && value<Short>() > 5 } },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideShort) && value<Short>() <= 5 } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_IntPrimitive() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnInteger,
            eq(3),
            { _, r -> r == null },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideInteger) && value<Int>() > 5 } },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideInteger) && value<Int>() <= 5 } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_LongPrimitive() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnLong,
            eq(3),
            { _, r -> r == null },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideLong) && value<Long>() > 5 } },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideLong) && value<Long>() <= 5 } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_FloatPrimitive() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnFloat,
            eq(3),
            { _, r -> r == null },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideFloat) && value<Float>() > 1f } },
            { mocks, _ ->
                mocks.single().run {
                    mocksMethod(Provider::provideFloat) && value<Float>().isNaN() || value<Float>() <= 1f
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_DoublePrimitive() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnDouble,
            eq(3),
            { _, r -> r == null },
            { mocks, _ -> mocks.single().run { mocksMethod(Provider::provideDouble) && value<Double>() > 1.0 } },
            { mocks, _ ->
                mocks.single().run {
                    mocksMethod(Provider::provideDouble) && value<Double>().isNaN() || value<Double>() <= 1.0
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_returnObject() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnObject,
            eq(4),
            { _, r -> r == null },
            { mocks, _ ->
                mocks.single().run {
                    mocksMethod(Provider::provideObject) && value<ExampleClass?>() == null
                }
            },
            { mocks, result ->
                val mockConstraint = mocks.single().run {
                    mocksMethod(Provider::provideObject) && value<ExampleClass>().field == 0
                }
                mockConstraint && result == 1
            },
            { mocks, result ->
                val mockConstraint = mocks.single().run {
                    mocksMethod(Provider::provideObject) && value<ExampleClass>().field != 0
                }
                mockConstraint && result == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_overloadedMethods() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnOverloadedMethods,
            eq(3),
            { _, _, r -> r == null },
            { _, mocks, result ->
                val zeroMockConstraint = mocks[0].run {
                    val mockFunc: Provider.() -> Int = Provider::provideOverloaded
                    val overloadedFunc: Provider.(Int) -> Int = Provider::provideOverloaded
                    mocksMethod(mockFunc) && !mocksMethod(overloadedFunc)
                }
                val firstMockConstraint = mocks[1].run {
                    val mockFunc: Provider.(Int) -> Int = Provider::provideOverloaded
                    val overloadedFunc: Provider.() -> Int = Provider::provideOverloaded
                    mocksMethod(mockFunc) && !mocksMethod(overloadedFunc)
                }
                val valueConstraint = mocks[0].value<Int>() < mocks[1].value<Int>()

                zeroMockConstraint && firstMockConstraint && valueConstraint && result == 1
            },

            { _, mocks, result ->
                val zeroMockConstraint = mocks[0].run {
                    val mockFunc: Provider.() -> Int = Provider::provideOverloaded
                    val overloadedFunc: Provider.(Int) -> Int = Provider::provideOverloaded
                    mocksMethod(mockFunc) && !mocksMethod(overloadedFunc)
                }
                val firstMockConstraint = mocks[1].run {
                    val mockFunc: Provider.(Int) -> Int = Provider::provideOverloaded
                    val overloadedFunc: Provider.() -> Int = Provider::provideOverloaded
                    mocksMethod(mockFunc) && !mocksMethod(overloadedFunc)
                }
                val valueConstraint = mocks[0].value<Int>() >= mocks[1].value<Int>()

                zeroMockConstraint && firstMockConstraint && valueConstraint && result == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForStaticField_objectArguments() {
        checkMocks(
            ServiceWithStaticField::calculateBasedOnObjectArgument,
            eq(4),
            { _, _, r -> r == null },
            { obj, _, _ -> obj == null },
            { obj, _, _ -> obj != null },
            { _, mocks, _ ->
                mocks.single().run { mocksMethod(Provider::provideGivenObject) && value<Int>() < 1 }
            },
            { _, mocks, _ ->
                mocks.single().run { mocksMethod(Provider::provideGivenObject) && value<Int>() >= 1 }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }
}