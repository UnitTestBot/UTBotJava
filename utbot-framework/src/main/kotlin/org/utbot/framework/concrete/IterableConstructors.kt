package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConstructorId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.booleanClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.util.valueToClassId

internal class CollectionConstructor : UtAssembleModelConstructorBase() {
    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface,
        value: Any
    ): List<UtStatementModel> {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        value as java.util.Collection<*>

        // If [value] constructed incorrectly (some inner transient fields are null, etc.) this may fail.
        // This value will be constructed as UtCompositeModel.
        val models = value.map { internalConstructor.construct(it, valueToClassId(it)) }

        val classId = value::class.java.id

        val addMethodId = MethodId(classId, "add", booleanClassId, listOf(objectClassId))

        return models.map { UtExecutableCallModel(this, addMethodId, listOf(it)) }
    }

    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        classId: ClassId
    ): UtExecutableCallModel =
        UtExecutableCallModel(
            instance = null,
            ConstructorId(classId, emptyList()),
            emptyList()
        )
}

internal class MapConstructor : UtAssembleModelConstructorBase() {
    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        classId: ClassId
    ): UtExecutableCallModel =
        UtExecutableCallModel(
            instance = null,
            ConstructorId(classId, emptyList()),
            emptyList()
        )

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface,
        value: Any
    ): List<UtStatementModel> {
        value as java.util.AbstractMap<*, *>

        val keyToValueModels = value.map { (key, value) ->
            internalConstructor.run { construct(key, valueToClassId(key)) to construct(value, valueToClassId(value)) }
        }

        val putMethodId = MethodId(classId, "put", objectClassId, listOf(objectClassId, objectClassId))

        return keyToValueModels.map { (key, value) ->
            UtExecutableCallModel(this, putMethodId, listOf(key, value))
        }
    }
}