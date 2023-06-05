package org.utbot.engine

import org.utbot.engine.overrides.stream.UtDoubleStream
import org.utbot.engine.overrides.stream.UtIntStream
import org.utbot.engine.overrides.stream.UtLongStream
import org.utbot.engine.overrides.stream.UtStream
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtStatementCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.doubleArrayClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.doubleStreamClassId
import org.utbot.framework.plugin.api.util.intArrayClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.intStreamClassId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.longArrayClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.longStreamClassId
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.streamClassId
import org.utbot.framework.util.nextModelName

/**
 * Auxiliary enum class for specifying an implementation for [StreamWrapper], that it will use.
 */
// TODO introduce a base interface for all such enums after https://github.com/UnitTestBot/UTBotJava/issues/146?
enum class UtStreamClass {
    UT_STREAM,
    UT_INT_STREAM,
    UT_LONG_STREAM,
    UT_DOUBLE_STREAM;

    val className: String
        get() = when (this) {
            UT_STREAM -> UtStream::class.java.canonicalName
            UT_INT_STREAM -> UtIntStream::class.java.canonicalName
            UT_LONG_STREAM -> UtLongStream::class.java.canonicalName
            UT_DOUBLE_STREAM -> UtDoubleStream::class.java.canonicalName
        }

    val elementClassId: ClassId
        get() = when (this) {
            UT_STREAM -> objectClassId
            UT_INT_STREAM -> intClassId
            UT_LONG_STREAM -> longClassId
            UT_DOUBLE_STREAM -> doubleClassId
        }

    val overriddenStreamClassId: ClassId
        get() = when (this) {
            UT_STREAM -> streamClassId
            UT_INT_STREAM -> intStreamClassId
            UT_LONG_STREAM -> longStreamClassId
            UT_DOUBLE_STREAM -> doubleStreamClassId
        }
}

abstract class StreamWrapper(
    utStreamClass: UtStreamClass, protected val elementsClassId: ClassId
) : BaseGenericStorageBasedContainerWrapper(utStreamClass.className) {
    protected val streamClassId = utStreamClass.overriddenStreamClassId

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtAssembleModel = resolver.run {
        val addr = holder.concreteAddr(wrapper.addr)
        val modelName = nextModelName(baseModelName)
        val parametersArrayModel = resolveElementsAsArrayModel(wrapper)?.transformElementsModel()

        val (builder, params) = if (parametersArrayModel == null || parametersArrayModel.length == 0) {
            streamEmptyMethodId to emptyList()
        } else {
            streamOfMethodId to listOf(parametersArrayModel)
        }

        val instantiationCall = UtStatementCallModel(
            instance = null,
            statement = builder,
            params = params
        )

        UtAssembleModel(addr, streamClassId, modelName, instantiationCall)
    }

    override fun chooseClassIdWithConstructor(classId: ClassId): ClassId = error("No constructor for Stream")

    override val modificationMethodId: MethodId
        get() = error("No modification method for Stream")

    private fun Resolver.resolveElementsAsArrayModel(wrapper: ObjectValue): UtArrayModel? {
        val elementDataFieldId = FieldId(overriddenClass.type.classId, "elementData")

        return collectFieldModels(wrapper.addr, overriddenClass.type)[elementDataFieldId] as? UtArrayModel
    }

    open fun UtArrayModel.transformElementsModel(): UtArrayModel = this

    private val streamOfMethodId: MethodId
        get() = methodId(
            classId = streamClassId,
            name = "of",
            returnType = streamClassId,
            arguments = arrayOf(elementsClassId) // vararg
        )

    private val streamEmptyMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "empty",
        returnType = streamClassId,
        arguments = emptyArray()
    )
}

class CommonStreamWrapper : StreamWrapper(UtStreamClass.UT_STREAM, objectArrayClassId) {
    override val baseModelName: String = "stream"
}

abstract class PrimitiveStreamWrapper(
    utStreamClass: UtStreamClass,
    elementsClassId: ClassId
) : StreamWrapper(utStreamClass, elementsClassId) {
    init {
        require(elementsClassId.isArray) {
            "Elements $$elementsClassId of primitive Stream wrapper for $streamClassId are not arrays"
        }
    }

    /**
     * Transforms a model for an array of wrappers (Integer, Long, etc) to an array of corresponding primitives.
     */
    override fun UtArrayModel.transformElementsModel(): UtArrayModel {
        val primitiveConstModel = if (constModel is UtNullModel) {
            // UtNullModel is not allowed for primitive arrays
            elementsClassId.elementClassId!!.defaultValueModel()
        } else {
            constModel.wrapperModelToPrimitiveModel()
        }

        return copy(
            classId = elementsClassId,
            constModel = primitiveConstModel,
            stores = stores.mapValuesTo(mutableMapOf()) { it.value.wrapperModelToPrimitiveModel() }
        )
    }

    /**
     * Transforms [this] to [UtPrimitiveModel] if it is an [UtAssembleModel] for the corresponding wrapper
     * (primitive int and wrapper Integer, etc.), and throws an error otherwise.
     */
    private fun UtModel.wrapperModelToPrimitiveModel(): UtModel {
        require(this !is UtNullModel) {
            "Unexpected null value in wrapper for primitive stream ${this@PrimitiveStreamWrapper.streamClassId}"
        }

        require(classId.isPrimitiveWrapper && this is UtAssembleModel) {
            "Unexpected not wrapper assemble model $this for value in wrapper " +
                    "for primitive stream ${this@PrimitiveStreamWrapper.streamClassId}"
        }

        return (instantiationCall.params.firstOrNull() as? UtPrimitiveModel)
            ?: error("No primitive value parameter for wrapper constructor $instantiationCall in model $this " +
                    "in wrapper for primitive stream ${this@PrimitiveStreamWrapper.streamClassId}")
    }
}

class IntStreamWrapper : PrimitiveStreamWrapper(UtStreamClass.UT_INT_STREAM, intArrayClassId) {
    override val baseModelName: String = "intStream"
}

class LongStreamWrapper : PrimitiveStreamWrapper(UtStreamClass.UT_LONG_STREAM, longArrayClassId) {
    override val baseModelName: String = "longStream"
}

class DoubleStreamWrapper : PrimitiveStreamWrapper(UtStreamClass.UT_DOUBLE_STREAM, doubleArrayClassId) {
    override val baseModelName: String = "doubleStream"
}
