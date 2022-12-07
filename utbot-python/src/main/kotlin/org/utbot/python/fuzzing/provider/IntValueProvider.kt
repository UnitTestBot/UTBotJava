package org.utbot.python.fuzzing.provider

import org.utbot.fuzzing.Seed
import org.utbot.fuzzing.ValueProvider
import org.utbot.fuzzing.seeds.BitVectorValue
import org.utbot.fuzzing.seeds.Signed
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.api.python.util.pythonIntClassId
import org.utbot.python.fuzzing.PythonMethodDescription
import org.utbot.python.fuzzing.provider.utils.isAny
import org.utbot.python.newtyping.PythonConcreteCompositeTypeDescription
import org.utbot.python.newtyping.general.Type

object IntValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val meta = type.meta
        if (meta is PythonConcreteCompositeTypeDescription) {
            return meta.name.toString() == "builtins.int"
        }
        return type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence {
        val integerConstants = listOf(
            BitVectorValue.fromInt(0),
            BitVectorValue.fromInt(1),
            BitVectorValue.fromInt(-1),
            BitVectorValue.fromInt(101),
            BitVectorValue.fromInt(-101),
        ).asSequence()
        yieldIntegers(128, integerConstants) { toBigInteger().toString(10) }
    }
    private suspend fun SequenceScope<Seed<Type, PythonTreeModel>>.yieldIntegers(
        bits: Int,
        consts: Sequence<BitVectorValue> = emptySequence(),
        block: BitVectorValue.() -> String,
    ) {
        (consts.filter { it.size <= bits } + Signed.values().map { it.invoke(bits) }).forEach { vector ->
            yield(Seed.Known(vector) { PythonTreeModel(PythonTree.PrimitiveNode(pythonIntClassId, block(it)), pythonIntClassId) })
        }
    }

}