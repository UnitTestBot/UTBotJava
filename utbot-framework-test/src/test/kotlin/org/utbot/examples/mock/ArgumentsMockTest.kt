package org.utbot.examples.mock

import org.utbot.tests.infrastructure.UtValueTestCaseChecker
import org.utbot.tests.infrastructure.DoNotCalculate
import org.utbot.tests.infrastructure.between
import org.utbot.tests.infrastructure.isParameter
import org.utbot.examples.mock.provider.Provider
import org.utbot.examples.mock.service.impl.ExampleClass
import org.utbot.examples.mock.service.impl.ServiceWithArguments
import org.utbot.tests.infrastructure.mocksMethod
import org.utbot.tests.infrastructure.value
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_PACKAGES
import org.junit.jupiter.api.Test
import org.utbot.testcheckers.eq

internal class ArgumentsMockTest : UtValueTestCaseChecker(testClass = ServiceWithArguments::class) {
    @Test
    fun testMockForArguments_callMultipleMethods() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::callMultipleMethods,
            eq(3),
            { provider, _, _, _ -> provider == null },
            { _, mocks, _, r ->
                val firstMockConstraint = mocks[0].mocksMethod(Provider::provideInteger)
                val secondMockConstraint = mocks[1].mocksMethod(Provider::provideLong)
                val valueConstraint = mocks[0].value<Int>() < mocks[1].value<Long>()
                val mockedValues = mocks.all { it.isParameter(1) }

                firstMockConstraint && secondMockConstraint && valueConstraint && mockedValues && r == 1
            },
            { _, mocks, _, r ->
                val firstMockConstraint = mocks[0].mocksMethod(Provider::provideInteger)
                val secondMockConstraint = mocks[1].mocksMethod(Provider::provideLong)
                val valueConstraint = mocks[0].value<Int>() >= mocks[1].value<Long>()
                val mockedValue = mocks.all { it.isParameter(1) }

                firstMockConstraint && secondMockConstraint && valueConstraint && mockedValue && r == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForArguments_IntArgument() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnIntArgument,
            eq(3),
            { provider, _, _, _, _ -> provider == null },
            { _, _, mocks, _, r ->
                val singleMock = mocks.single()

                val mocksMethod = singleMock.mocksMethod(Provider::provideGiven)
                val valueConstraint = singleMock.value<Int>(0) < singleMock.value<Int>(1)
                val paramConstraint = singleMock.isParameter(1)

                mocksMethod && valueConstraint && paramConstraint && r == 1
            },
            { _, _, mocks, _, r ->
                val singleMock = mocks.single()

                val mocksMethod = singleMock.mocksMethod(Provider::provideGiven)
                val valueConstraint = singleMock.value<Int>(0) >= singleMock.value<Int>(1)
                val paramConstraint = singleMock.isParameter(1)

                mocksMethod && valueConstraint && paramConstraint && r == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForArguments_BooleanPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnBoolean,
            eq(3),
            { provider, _, _, _ -> provider == null },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideBoolean) && value() && isParameter(1) && r == 1
                }
            },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideBoolean) && !value<Boolean>() && isParameter(1) && r == 0
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForArguments_inconsistentBoolean() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::inconsistentBoolean,
            eq(4),
            { provider, _, _, _ -> provider == null },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideBoolean) && !value<Boolean>() && isParameter(1) && r == 0
                }
            },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideBoolean) && value(0) && value(1) && isParameter(1) && r == 0
                }
            },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideBoolean) && value(0) && !value<Boolean>(1) && isParameter(1) && r == 1
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }


    @Test
    fun testMockForArguments_CharacterPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnCharacter,
            eq(3),
            { provider, _, _, _ -> provider == null },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideCharacter) && value<Char>() > 'a' && isParameter(1) && r == 1
                }
            },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideCharacter) && value<Char>() <= 'a' && isParameter(1) && r == 0
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForArguments_BytePrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnByte,
            eq(3),
            { provider, _, _, _ -> provider == null },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideByte) && value<Byte>() > 5 && isParameter(1) && r == 1
                }
            },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideByte) && value<Byte>() <= 5 && isParameter(1) && r == 0
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForArguments_ShortPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnShort,
            eq(3),
            { provider, _, _, _ -> provider == null },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideShort) && value<Short>() > 5 && isParameter(1) && r == 1
                }
            },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideShort) && value<Short>() <= 5 && isParameter(1) && r == 0
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForArguments_IntPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnInteger,
            eq(3),
            { provider, _, _, _ -> provider == null },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideInteger) && value<Int>() > 5 && isParameter(1) && r == 1
                }
            },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideInteger) && value<Int>() <= 5 && isParameter(1) && r == 0
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForArguments_LongPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnLong,
            eq(3),
            { provider, _, _, _ -> provider == null },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideLong) && value<Long>() > 5 && isParameter(1) && r == 1
                }
            },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideLong) && value<Long>() <= 5 && isParameter(1) && r == 0
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Suppress("SimplifyNegatedBinaryExpression")
    @Test
    fun testMockForArguments_FloatPrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnFloat,
            eq(3),
            { provider, _, _, _ -> provider == null },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideFloat) && value<Float>() > 1f && isParameter(1) && r == 1
                }
            },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideFloat) && !(value<Float>() > 1f) && isParameter(1) && r == 0
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Suppress("SimplifyNegatedBinaryExpression")
    @Test
    fun testMockForArguments_DoublePrimitive() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnDouble,
            eq(3),
            { provider, _, _, _ -> provider == null },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideDouble) && value<Double>() > 1.0 && isParameter(1) && r == 1
                }
            },
            { _, mocks, _, r ->
                mocks.single().run {
                    mocksMethod(Provider::provideDouble) && !(value<Double>() > 1.0) && isParameter(1) && r == 0
                }
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForArguments_returnObject() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnObject,
            eq(4),
            { provider, _, _, _ -> provider == null },
            { _, mocks, _, _ ->
                mocks.single().run {
                    mocksMethod(Provider::provideObject) && value<ExampleClass?>() == null && isParameter(1)
                }
            },
            { _, mocks, _, r ->
                val mockConstraint = mocks.single().run {
                    mocksMethod(Provider::provideObject) && value<ExampleClass>().field == 0 && isParameter(1)
                }

                mockConstraint && r == 1
            },
            { _, mocks, _, r ->
                val mockConstraint = mocks.single().run {
                    mocksMethod(Provider::provideObject) && value<ExampleClass>().field != 0 && isParameter(1)
                }

                mockConstraint && r == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForArguments_overloadedMethods() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnOverloadedMethods,
            eq(3),
            { provider, _, _, _, r -> provider == null && r == null },
            { _, _, mocks, _, r ->
                val zeroMockConstraint = mocks[0].run {
                    val mockFunc: Provider.() -> Int = Provider::provideOverloaded
                    val overloadedFunc: Provider.(Int) -> Int = Provider::provideOverloaded
                    mocksMethod(mockFunc) && !mocksMethod(overloadedFunc) && isParameter(1)
                }
                val firstMockConstraint = mocks[1].run {
                    val mockFunc: Provider.(Int) -> Int = Provider::provideOverloaded
                    val overloadedFunc: Provider.() -> Int = Provider::provideOverloaded
                    mocksMethod(mockFunc) && !mocksMethod(overloadedFunc) && isParameter(1)
                }

                zeroMockConstraint && firstMockConstraint && mocks[0].value<Int>() < mocks[1].value<Int>() && r == 1
            },

            { _, _, mocks, _, r ->
                val zeroMockConstraint = mocks[0].run {
                    val mockFunc: Provider.() -> Int = Provider::provideOverloaded
                    val overloadedFunc: Provider.(Int) -> Int = Provider::provideOverloaded
                    mocksMethod(mockFunc) && !mocksMethod(overloadedFunc) && isParameter(1)
                }
                val firstMockConstraint = mocks[1].run {
                    val mockFunc: Provider.(Int) -> Int = Provider::provideOverloaded
                    val overloadedFunc: Provider.() -> Int = Provider::provideOverloaded
                    mocksMethod(mockFunc) && !mocksMethod(overloadedFunc) && isParameter(1)
                }

                zeroMockConstraint && firstMockConstraint && mocks[0].value<Int>() >= mocks[1].value<Int>() && r == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }

    @Test
    fun testMockForArguments_objectArguments() {
        checkMocksAndInstrumentation(
            ServiceWithArguments::calculateBasedOnObjectArgument,
            between(3..4),
            { provider, _, mocks, _, r -> provider == null && mocks.isEmpty() && r == null }, // NPE
            { _, obj, _, _, _ -> obj != null },
            { _, _, mocks, _, r ->
                val mockConstraint = mocks.single().run {
                    mocksMethod(Provider::provideGivenObject) && value<Int>() < 1 && isParameter(1)
                }
                mockConstraint && r == 1
            },
            { _, _, mocks, _, r ->
                val mockConstraint = mocks.single().run {
                    mocksMethod(Provider::provideGivenObject) && value<Int>() >= 1 && isParameter(1)
                }
                mockConstraint && r == 0
            },
            coverage = DoNotCalculate,
            mockStrategy = OTHER_PACKAGES
        )
    }
}