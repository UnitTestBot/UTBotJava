package org.utbot.examples.mock

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.between
import org.utbot.examples.mock.provider.Provider
import org.utbot.examples.mock.service.impl.ExampleClass
import org.utbot.examples.mock.service.impl.ServiceWithField
import org.utbot.tests.infrastructure.mocksMethod
import org.utbot.tests.infrastructure.value
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_PACKAGES
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class FieldMockTest : UtValueTestCaseChecker(testClass = ServiceWithField::class) {
    @Test
    fun testMockForField_callMultipleMethods() {
        checkMocksAndInstrumentation(
            ServiceWithField::callMultipleMethods,
            eq(3),
            { _, _, r -> r == null },
            { mocks, _, r ->
                val zeroMock = mocks[0].mocksMethod(Provider::provideInteger)
                val firstMock = mocks[1].mocksMethod(Provider::provideLong)
                val valueConstraint = mocks[0].value<Int>() < mocks[1].value<Long>()

                zeroMock && firstMock && valueConstraint && r == 1
            },
            { mocks, _, r ->
                val zeroMock = mocks[0].mocksMethod(Provider::provideInteger)
                val firstMock = mocks[1].mocksMethod(Provider::provideLong)
                val valueConstraint = mocks[0].value<Int>() >= mocks[1].value<Long>()

                zeroMock && firstMock && valueConstraint && r == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForField_IntArgument() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnIntArgument,
            eq(3),
            { _, _, _, r -> r == null },
            { _, mocks, _, _ ->
                mocks.single().run {
                    mocksMethod(Provider::provideGiven) && value<Int>(0) < value<Int>(1)
                }
            },
            { _, mocks, _, _ ->
                mocks.single().run {
                    mocksMethod(Provider::provideGiven) && value<Int>(0) >= value<Int>(1)
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForField_BooleanPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnBoolean,
            eq(3),
            { _, _, r -> r == null },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideBoolean) && value() } },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideBoolean) && !value<Boolean>() } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForField_inconsistentBoolean() {
        checkMocksAndInstrumentation(
            ServiceWithField::inconsistentBoolean,
            eq(4),
            { _, _, r -> r == null },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideBoolean) && !value<Boolean>() } },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideBoolean) && value(0) && value(1) } },
            { mocks, _, _ ->
                mocks.single().run { mocksMethod(Provider::provideBoolean) && value(0) && !value<Boolean>(1) }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }


    @Test
    fun testMockForField_CharacterPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnCharacter,
            eq(3),
            { _, _, r -> r == null },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideCharacter) && value<Char>() > 'a' } },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideCharacter) && value<Char>() <= 'a' } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForField_BytePrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnByte,
            eq(3),
            { _, _, r -> r == null },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideByte) && value<Byte>() > 5 } },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideByte) && value<Byte>() <= 5 } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForField_ShortPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnShort,
            eq(3),
            { _, _, r -> r == null },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideShort) && value<Short>() > 5 } },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideShort) && value<Short>() <= 5 } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForField_IntPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnInteger,
            eq(3),
            { _, _, r -> r == null },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideInteger) && value<Int>() > 5 } },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideInteger) && value<Int>() <= 5 } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForField_LongPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnLong,
            eq(3),
            { _, _, r -> r == null },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideLong) && value<Long>() > 5 } },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideLong) && value<Long>() <= 5 } },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForField_FloatPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnFloat,
            eq(3),
            { _, _, r -> r == null },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideFloat) && value<Float>() > 1f } },
            { mocks, _, _ ->
                mocks.single().run {
                    mocksMethod(Provider::provideFloat) && value<Float>().isNaN() || value<Float>() <= 1f
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForField_DoublePrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnDouble,
            eq(3),
            { _, _, r -> r == null },
            { mocks, _, _ -> mocks.single().run { mocksMethod(Provider::provideDouble) && value<Double>() > 1.0 } },
            { mocks, _, _ ->
                mocks.single().run {
                    mocksMethod(Provider::provideDouble) && value<Double>().isNaN() || value<Double>() <= 1.0
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForField_returnObject() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnObject,
            eq(4),
            { _, _, r -> r == null },
            { mocks, _, _ ->
                mocks.single().run {
                    mocksMethod(Provider::provideObject) && value<ExampleClass?>() == null
                }
            },
            { mocks, _, result ->
                val mockConstraint = mocks.single().run {
                    mocksMethod(Provider::provideObject) && value<ExampleClass>().field == 0
                }

                mockConstraint && result == 1
            },
            { mocks, _, result ->
                val mockConstraint = mocks.single().run {
                    mocksMethod(Provider::provideObject) &&
                            value<ExampleClass>().field != 0
                }

                mockConstraint && result == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForField_overloadedMethods() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnOverloadedMethods,
            eq(3),
            { _, _, _, r -> r == null },
            { _, mocks, _, result ->
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
            { _, mocks, _, result ->
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
    fun testMockForField_objectArguments() {
        checkMocksAndInstrumentation(
            ServiceWithField::calculateBasedOnObjectArgument,
            between(3..4),
            { _, _, _, r -> r == null },
            { obj, _, _, _ -> obj == null },
            { obj, _, _, _ -> obj != null },
            { _, mocks, _, r ->
                val mockConstraint = mocks.single().run {
                    mocksMethod(Provider::provideGivenObject) && value<Int>() < 1
                }
                mockConstraint && r == 1
            },
            { _, mocks, _, r ->
                val mockConstraint = mocks.single().run {
                    mocksMethod(Provider::provideGivenObject) && value<Int>() >= 1
                }

                mockConstraint && r == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }
}