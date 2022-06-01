package org.utbot.framework.plugin.api

import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.byteClassId
import org.utbot.framework.plugin.api.util.charClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.floatClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.shortClassId
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.framework.plugin.api.util.voidClassId
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.fuzzer.FuzzedConcreteValue
import org.utbot.fuzzer.FuzzedMethodDescription
import org.utbot.fuzzer.FuzzedOp
import org.utbot.fuzzer.ModelProvider
import org.utbot.fuzzer.providers.ConstantsModelProvider
import org.utbot.fuzzer.providers.ObjectModelProvider
import org.utbot.fuzzer.providers.PrimitivesModelProvider
import org.utbot.fuzzer.providers.StringConstantModelProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.utbot.fuzzer.defaultModelProviders
import org.utbot.fuzzer.providers.PrimitiveDefaultsModelProvider
import java.util.Date

class ModelProviderTest {

    @Test
    fun `test generate primitive models for boolean`() {
        val models = collect(PrimitivesModelProvider,
            parameters = listOf(booleanClassId)
        )

        assertEquals(1, models.size)
        assertEquals(2, models[0]!!.size)
        assertTrue(models[0]!!.contains(UtPrimitiveModel(true)))
        assertTrue(models[0]!!.contains(UtPrimitiveModel(false)))
    }

    @Test
    fun `test all known primitive types are generate at least one value`() {
        val primitiveTypes = listOf(
            byteClassId,
            booleanClassId,
            charClassId,
            shortClassId,
            intClassId,
            longClassId,
            floatClassId,
            doubleClassId,
            stringClassId,
        )
        val models = collect(PrimitivesModelProvider,
            parameters = primitiveTypes
        )

        assertEquals(primitiveTypes.size, models.size)
        primitiveTypes.indices.forEach {
            assertTrue(models[it]!!.isNotEmpty())
        }
    }

    @Test
    fun `test that empty constants don't generate any models`() {
        val models = collect(ConstantsModelProvider,
            parameters = listOf(intClassId),
            constants = emptyList()
        )

        assertEquals(0, models.size)
    }

    @Test
    fun `test that one constant generate corresponding value`() {
        val models = collect(ConstantsModelProvider,
            parameters = listOf(intClassId),
            constants = listOf(
                FuzzedConcreteValue(intClassId, 123)
            )
        )

        assertEquals(1, models.size)
        assertEquals(1, models[0]!!.size)
        assertEquals(UtPrimitiveModel(123), models[0]!![0])
        assertEquals(intClassId, models[0]!![0].classId)
    }

    @Test
    fun `test that constants are mutated if comparison operation is set`() {
        val models = collect(ConstantsModelProvider,
            parameters = listOf(intClassId),
            constants = listOf(
                FuzzedConcreteValue(intClassId, 10, FuzzedOp.EQ),
                FuzzedConcreteValue(intClassId, 20, FuzzedOp.NE),
                FuzzedConcreteValue(intClassId, 30, FuzzedOp.LT),
                FuzzedConcreteValue(intClassId, 40, FuzzedOp.LE),
                FuzzedConcreteValue(intClassId, 50, FuzzedOp.GT),
                FuzzedConcreteValue(intClassId, 60, FuzzedOp.GE),
            )
        )

        assertEquals(1, models.size)
        val expectedValues = listOf(10, 11, 20, 21, 29, 30, 40, 41, 50, 51, 59, 60)
        assertEquals(expectedValues.size, models[0]!!.size)
        expectedValues.forEach {
            assertTrue(models[0]!!.contains(UtPrimitiveModel(it)))
        }
    }

    @Test
    fun `test constant empty string generates only corresponding model`() {
        val models = collect(StringConstantModelProvider,
            parameters = listOf(stringClassId),
            constants = listOf(
                FuzzedConcreteValue(stringClassId, "", FuzzedOp.CH),
            )
        )

        assertEquals(1, models.size)
        assertEquals(1, models[0]!!.size)
        assertEquals(UtPrimitiveModel(""), models[0]!![0])
    }

    @Test
    fun `test non-empty string is not mutated if operation is not set`() {
        val models = collect(StringConstantModelProvider,
            parameters = listOf(stringClassId),
            constants = listOf(
                FuzzedConcreteValue(stringClassId, "nonemptystring", FuzzedOp.NONE),
            )
        )

        assertEquals(1, models.size)
        assertEquals(1, models[0]!!.size)
        assertEquals(UtPrimitiveModel("nonemptystring"), models[0]!![0])
    }

    @Test
    fun `test non-empty string is mutated if modification operation is set`() {
        val models = collect(StringConstantModelProvider,
            parameters = listOf(stringClassId),
            constants = listOf(
                FuzzedConcreteValue(stringClassId, "nonemptystring", FuzzedOp.CH),
            )
        )

        assertEquals(1, models.size)
        assertEquals(2, models[0]!!.size)
        listOf("nonemptystring", "nonemptystr`ng").forEach {
            assertTrue( models[0]!!.contains(UtPrimitiveModel(it))) { "Failed to find string $it in list ${models[0]}" }
        }
    }

    @Test
    fun `test mutation creates the same values between different runs`() {
        repeat(10) {
            val models = collect(StringConstantModelProvider,
                parameters = listOf(stringClassId),
                constants = listOf(
                    FuzzedConcreteValue(stringClassId, "anotherstring", FuzzedOp.CH),
                )
            )
            listOf("anotherstring", "anotherskring").forEach {
                assertTrue( models[0]!!.contains(UtPrimitiveModel(it))) { "Failed to find string $it in list ${models[0]}" }
            }
        }
    }

    @Test
    @Suppress("unused", "UNUSED_PARAMETER", "RemoveEmptySecondaryConstructorBody")
    fun `test default object model creation for simple constructors`() {
        withUtContext(UtContext(this::class.java.classLoader)) {
            class A {
                constructor(a: Int) {}
                constructor(a: Int, b: String) {}
                constructor(a: Int, b: String, c: Boolean)
            }

            val classId = A::class.java.id
            val models = collect(
                ObjectModelProvider { 0 }.apply {
                    modelProvider = ModelProvider.of(PrimitiveDefaultsModelProvider)
                },
                parameters = listOf(classId)
            )

            assertEquals(1, models.size)
            assertEquals(3, models[0]!!.size)
            assertTrue(models[0]!!.all { it is UtAssembleModel && it.classId == classId })

            models[0]!!.filterIsInstance<UtAssembleModel>().forEachIndexed { index, model ->
                assertEquals(1, model.instantiationChain.size)
                val stm = model.instantiationChain[0]
                assertTrue(stm is UtExecutableCallModel)
                stm as UtExecutableCallModel
                val paramCountInConstructorAsTheyListed = index + 1
                assertEquals(paramCountInConstructorAsTheyListed, stm.params.size)
            }
        }
    }

    @Test
    fun `test no object model is created for empty constructor`() {
        withUtContext(UtContext(this::class.java.classLoader)) {
            class A

            val classId = A::class.java.id
            val models = collect(
                ObjectModelProvider { 0 },
                parameters = listOf(classId)
            )

            assertEquals(1, models.size)
            assertEquals(1, models[0]!!.size)
        }
    }

    @Test
    @Suppress("unused", "UNUSED_PARAMETER")
    fun `test that constructors with not primitive parameters are ignored`() {
        withUtContext(UtContext(this::class.java.classLoader)) {
            class A {
                constructor(a: Int, b: Int)
                constructor(a: Int, b: Date)
            }

            val classId = A::class.java.id
            val models = collect(
                ObjectModelProvider { 0 },
                parameters = listOf(classId)
            )

            assertEquals(1, models.size)
            assertTrue(models[0]!!.isNotEmpty())
            val chain = (models[0]!![0] as UtAssembleModel).instantiationChain
            assertEquals(1, chain.size)
            assertTrue(chain[0] is UtExecutableCallModel)
            (chain[0] as UtExecutableCallModel).params.forEach {
                assertEquals(intClassId, it.classId)
            }
        }
    }

    @Test
    fun `test fallback model can create custom values for any parameter`() {
        val firstParameterIsUserGenerated = ModelProvider { _, consumer ->
            consumer.accept(0, UtPrimitiveModel(-123))
        }.withFallback(PrimitivesModelProvider)

        val result = collect(
            firstParameterIsUserGenerated,
            parameters = listOf(intClassId, intClassId)
        )

        assertEquals(2, result.size)
        assertEquals(1, result[0]!!.size)
        assertTrue(result[1]!!.size > 1)
        assertEquals(UtPrimitiveModel(-123), result[0]!![0])
    }

    @Test
    fun `test collection model can produce basic values with assembled model`() {
        withUtContext(UtContext(this::class.java.classLoader)) {
            val result = collect(
                defaultModelProviders { 0 },
                parameters = listOf(java.util.List::class.java.id)
            )

            assertEquals(1, result.size)
        }
    }

    private fun collect(
        modelProvider: ModelProvider,
        name: String = "testMethod",
        returnType: ClassId = voidClassId,
        parameters: List<ClassId>,
        constants: List<FuzzedConcreteValue> = emptyList()
    ): Map<Int, List<UtModel>> {
        return mutableMapOf<Int, MutableList<UtModel>>().apply {
            modelProvider.generate(FuzzedMethodDescription(name, returnType, parameters, constants)) { i, m ->
                computeIfAbsent(i) { mutableListOf() }.add(m)
            }
        }
    }

}