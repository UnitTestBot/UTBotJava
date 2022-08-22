package org.utbot.framework.plugin.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedOp
import org.utbot.fuzzer.FuzzedValue
import org.utbot.fuzzer.providers.ConstantsModelProvider

class FuzzedValueDescriptionTest {

    @Test
    fun testConstantModelProviderTest() {
        val values = mutableListOf<FuzzedValue>()
        val concreteValues = listOf(
            FuzzedConcreteValue(intClassId, 10, FuzzedOp.EQ),
            FuzzedConcreteValue(intClassId, 20, FuzzedOp.NE),
            FuzzedConcreteValue(intClassId, 30, FuzzedOp.LT),
            FuzzedConcreteValue(intClassId, 40, FuzzedOp.LE),
            FuzzedConcreteValue(intClassId, 50, FuzzedOp.GT),
            FuzzedConcreteValue(intClassId, 60, FuzzedOp.GE),
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
                returnType = voidClassId,
                parameters = listOf(intClassId),
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