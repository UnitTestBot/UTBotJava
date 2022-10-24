package org.utbot.framework.concrete

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.util.doubleArrayClassId
import org.utbot.framework.plugin.api.util.doubleStreamClassId
import org.utbot.framework.plugin.api.util.intArrayClassId
import org.utbot.framework.plugin.api.util.intStreamClassId
import org.utbot.framework.plugin.api.util.longArrayClassId
import org.utbot.framework.plugin.api.util.longStreamClassId
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.streamClassId
import org.utbot.framework.util.valueToClassId

/**
 * Max number of elements in any concrete stream.
 */
private const val STREAM_ELEMENTS_LIMIT: Int = 1_000_000

internal abstract class AbstractStreamConstructor(private val streamClassId: ClassId, elementsClassId: ClassId) : UtAssembleModelConstructorBase() {
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

        return UtExecutableCallModel(
            instance = null,
            executable = ofMethodId,
            params = models
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
}

internal class BaseStreamConstructor : AbstractStreamConstructor(streamClassId, objectArrayClassId)
internal class IntStreamConstructor : AbstractStreamConstructor(intStreamClassId, intArrayClassId)
internal class LongStreamConstructor : AbstractStreamConstructor(longStreamClassId, longArrayClassId)
internal class DoubleStreamConstructor : AbstractStreamConstructor(doubleStreamClassId, doubleArrayClassId)
