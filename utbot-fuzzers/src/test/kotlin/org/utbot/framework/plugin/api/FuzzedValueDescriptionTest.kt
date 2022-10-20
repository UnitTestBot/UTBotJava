package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedContext
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.ConstantsModelProvider
import org.utbot.fuzzer.types.JavaInt
import org.utbot.fuzzer.types.JavaVoid

class FuzzedValueDescriptionTest {

    @Test
    fun testConstantModelProviderTest() {
        val values = mutableListOf<FuzzedValue>()
        val concreteValues = listOf(
            FuzzedConcreteValue(JavaInt, 10, FuzzedContext.Comparison.EQ),
            FuzzedConcreteValue(JavaInt, 20, FuzzedContext.Comparison.NE),
            FuzzedConcreteValue(JavaInt, 30, FuzzedContext.Comparison.LT),
            FuzzedConcreteValue(JavaInt, 40, FuzzedContext.Comparison.LE),
            FuzzedConcreteValue(JavaInt, 50, FuzzedContext.Comparison.GT),
            FuzzedConcreteValue(JavaInt, 60, FuzzedContext.Comparison.GE),
        )
        val summaries = listOf(
            "%var% = 10" to 10,
            "%var% != 10" to 11, // 1, FuzzedOp.EQ -> %var% == 10: False
            "%var% = 20" to 20,
            "%var% != 20" to 21, // 2, FuzzedOp.NE -> %var% != 20: True
            "%var% = 30" to 30,
            "%var% < 30" to 29, // 3, FuzzedOp.LT -> %var% < 30: True
            "%var% = 40" to 40,
            "%var% > 40" to 41, // 4, FuzzedOp.LE -> %var% <= 40: False
            "%var% = 50" to 50,
            "%var% > 50" to 51, // 5, FuzzedOp.GT -> %var% > 50: True
            "%var% = 60" to 60,
            "%var% < 60" to 59, // 6, FuzzedOp.GE -> %var% >= 60: False
        )
        val expected = concreteValues.size * 2
        ConstantsModelProvider.generate(
            FuzzedMethodDescription(
                name = "name",
                returnType = JavaVoid,
                parameters = listOf(JavaInt),
                concreteValues = concreteValues
            )
        ).forEach { (_, value) -> values.add(value) }
        assertEquals(expected, values.size) {
            "Expected $expected values: a half is origin values and another is modified, but only ${values.size} are generated"
        }
        for (i in summaries.indices) {
            assertEquals(summaries[i].second, (values[i].model as UtPrimitiveModel).value) {
                "Constant model provider should change constant values to reverse if-statement"
            }
            assertEquals(summaries[i].first, values[i].summary)
        }
    }

}