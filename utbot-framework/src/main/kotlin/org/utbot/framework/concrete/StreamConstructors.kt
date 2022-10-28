package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.doubleArrayClassId
import org.utbot.framework.plugin.api.util.doubleStreamClassId
import org.utbot.framework.plugin.api.util.intArrayClassId
import org.utbot.framework.plugin.api.util.intStreamClassId
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.longArrayClassId
import org.utbot.framework.plugin.api.util.longStreamClassId
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.streamClassId
import org.utbot.framework.util.modelIdCounter
import org.utbot.framework.util.valueToClassId

/**
 * Max number of elements in any concrete stream.
 */
private const val STREAM_ELEMENTS_LIMIT: Int = 1_000_000

internal abstract class AbstractStreamConstructor(private val streamClassId: ClassId, private val elementsClassId: ClassId) : UtAssembleModelConstructorBase() {
    private val singleElementClassId: ClassId = elementsClassId.elementClassId
        ?: error("Stream $streamClassId elements have to be an array but $elementsClassId found")

    private val elementDefaultValueModel: UtModel = singleElementClassId.defaultValueModel()

    override fun provideInstantiationCall(
        internalConstructor: UtModelConstructorInterface,
        value: Any,
        classId: ClassId
    ): UtExecutableCallModel {
        value as java.util.stream.BaseStream<*, *>

        // If [value] constructed incorrectly (some inner transient fields are null, etc.) this may fail.
        // This value will be constructed as UtCompositeModel.
        val models = value
            .iterator()
            .asSequence()
            .map { internalConstructor.construct(it, valueToClassId(it)) }
            .take(STREAM_ELEMENTS_LIMIT)
            .toList()

        if (models.isEmpty()) {
            return UtExecutableCallModel(
                instance = null,
                executable = emptyMethodId,
                params = emptyList()
            )
        }

        val varargModelsArray = UtArrayModel(
            id = modelIdCounter.incrementAndGet(),
            classId = elementsClassId,
            length = models.size,
            constModel = elementDefaultValueModel,
            stores = models.mapIndexed { i, model -> i to model.wrapperModelToPrimitiveModel() }.toMap(mutableMapOf())
        )

        return UtExecutableCallModel(
            instance = null,
            executable = ofMethodId,
            params = listOf(varargModelsArray)
        )
    }

    override fun UtAssembleModel.provideModificationChain(
        internalConstructor: UtModelConstructorInterface,
        value: Any
    ): List<UtStatementModel> = emptyList()

    private val emptyMethodId: MethodId = methodId(
        classId = this.streamClassId,
        name = "empty",
        returnType = this.streamClassId,
        arguments = emptyArray()
    )

    private val ofMethodId: MethodId = methodId(
        classId = this.streamClassId,
        name = "of",
        returnType = this.streamClassId,
        arguments = arrayOf(elementsClassId) // vararg
    )

    /**
     * Transforms [this] to [UtPrimitiveModel] if it is an [UtAssembleModel] for the corresponding wrapper
     * (primitive int and wrapper Integer, etc.), and throws an error otherwise.
     */
    private fun UtModel.wrapperModelToPrimitiveModel(): UtModel {
        if (!classId.isPrimitiveWrapper) {
            // We do not need to transform classes other than primitive wrappers
            return this
        }

        require(this !is UtNullModel) {
            "Unexpected null value in wrapper for primitive stream ${this@AbstractStreamConstructor}"
        }

        require(this is UtAssembleModel) {
            "Unexpected not wrapper assemble model $this for value in wrapper " +
                    "for primitive stream ${this@AbstractStreamConstructor.streamClassId}"
        }

        return (instantiationCall.params.firstOrNull() as? UtPrimitiveModel)
            ?: error("No primitive value parameter for wrapper constructor $instantiationCall in model $this " +
                    "in wrapper for primitive stream ${this@AbstractStreamConstructor.streamClassId}")
    }
}

internal class BaseStreamConstructor : AbstractStreamConstructor(streamClassId, objectArrayClassId)
internal class IntStreamConstructor : AbstractStreamConstructor(intStreamClassId, intArrayClassId)
internal class LongStreamConstructor : AbstractStreamConstructor(longStreamClassId, longArrayClassId)
internal class DoubleStreamConstructor : AbstractStreamConstructor(doubleStreamClassId, doubleArrayClassId)
