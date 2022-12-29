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
import java.math.BigInteger

object IntValueProvider : ValueProvider<Type, PythonTreeModel, PythonMethodDescription> {
    override fun accept(type: Type): Boolean {
        val meta = type.meta
        if (meta is PythonConcreteCompositeTypeDescription) {
            return meta.name.toString() == "builtins.int"
        }
        return type.isAny()
    }

    override fun generate(description: PythonMethodDescription, type: Type) = sequence<Seed<Type, PythonTreeModel>> {
        val bits = 128
        val integerConstants = listOf(
            BitVectorValue.fromBigInteger(BigInteger("239")),
            BitVectorValue.fromBigInteger(BigInteger("100")),
            BitVectorValue.fromBigInteger(BigInteger("-100")),
        ).asSequence()
        (integerConstants + Signed.values().map { it.invoke(bits) }).forEach { vector ->
            yield(Seed.Known(vector) {
                PythonTreeModel(PythonTree.PrimitiveNode(pythonIntClassId, it.toBigInteger().toString(10)), pythonIntClassId)
            })
        }
    }
}