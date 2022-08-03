package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.*
import org.utbot.framework.util.valueToClassId

internal class CollectionConstructor : UtAssembleModelConstructorBase() {
    override fun UtAssembleModel.modifyChains(
        internalConstructor: UtModelConstructorInterface,
        instantiationChain: MutableList<UtStatementModel>,
        modificationChain: MutableList<UtStatementModel>,
        valueToConstructFrom: Any
    ) {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        valueToConstructFrom as java.util.Collection<*>

        // If [valueToConstructFrom] constructed incorrectly (some inner transient fields are null, etc.) this may fail.
        // This value will be constructed as UtCompositeModel.
        val models = valueToConstructFrom.map { internalConstructor.construct(it, valueToClassId(it)) }

        val classId = valueToConstructFrom::class.java.id

        instantiationChain += UtExecutableCallModel(
            instance = null,
            classId.findConstructor().asExecutable(),
            emptyList(),
            this
        )

        val addMethodId = classId.findMethod("add", booleanClassId, listOf(objectClassId)).asExecutableMethod()

        modificationChain += models.map { UtExecutableCallModel(this, addMethodId, listOf(it)) }
    }
}

internal class MapConstructor : UtAssembleModelConstructorBase() {
    override fun UtAssembleModel.modifyChains(
        internalConstructor: UtModelConstructorInterface,
        instantiationChain: MutableList<UtStatementModel>,
        modificationChain: MutableList<UtStatementModel>,
        valueToConstructFrom: Any
    ) {
        valueToConstructFrom as java.util.AbstractMap<*, *>

        val keyToValueModels = valueToConstructFrom.map { (key, value) ->
            internalConstructor.run { construct(key, valueToClassId(key)) to construct(value, valueToClassId(value)) }
        }

        instantiationChain += UtExecutableCallModel(
            instance = null,
            classId.findConstructor().asExecutable(),
            emptyList(),
            this
        )

        val putMethodId = classId.findMethod("put", objectClassId, listOf(objectClassId, objectClassId)).asExecutableMethod()

        modificationChain += keyToValueModels.map { (key, value) ->
            UtExecutableCallModel(this, putMethodId, listOf(key, value))
        }
    }
}