package examples.unsafe

import examples.CustomJavaDocTagsEnabler
import examples.SummaryTestCaseGeneratorTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.utbot.examples.unsafe.UnsafeWithField
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.testing.DoNotCalculate

@ExtendWith(CustomJavaDocTagsEnabler::class)
class UnsafeWithFieldTest : SummaryTestCaseGeneratorTest(
    UnsafeWithField::class
) {
    @Test
    fun testUnsafeWithField() {
        val summary1 = "@utbot.classUnderTest {@link UnsafeWithField}\n" +
                "@utbot.methodUnderTest {@link org.utbot.examples.unsafe.UnsafeWithField#setField(java.text.NumberFormat.Field)}\n" +
                "@utbot.returnsFrom {@code return Field.INTEGER;}"

        val methodName1 = "testSetField_ReturnFieldINTEGER"

        val displayName1 = "-> return Field.INTEGER"

        val summaryKeys = listOf(
            summary1
        )

        val displayNames = listOf(
            displayName1
        )

        val methodNames = listOf(
            methodName1
        )

        val method = UnsafeWithField::setField
        val mockStrategy = MockStrategyApi.NO_MOCKS
        val coverage = DoNotCalculate

        summaryCheck(method, mockStrategy, coverage, summaryKeys, methodNames, displayNames)
    }
}