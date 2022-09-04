package org.utbot.engine

import org.utbot.engine.overrides.stream.UtDoubleStream
import org.utbot.engine.overrides.stream.UtIntStream
import org.utbot.engine.overrides.stream.UtLongStream
import org.utbot.engine.overrides.stream.UtStream
import org.utbot.engine.overrides.stream.actions.objects.ConsumerAction
import org.utbot.engine.overrides.stream.actions.DistinctAction
import org.utbot.engine.overrides.stream.actions.objects.FilterAction
import org.utbot.engine.overrides.stream.actions.LimitAction
import org.utbot.engine.overrides.stream.actions.objects.MapAction
import org.utbot.engine.overrides.stream.actions.NaturalSortingAction
import org.utbot.engine.overrides.stream.actions.SkipAction
import org.utbot.engine.overrides.stream.actions.objects.SortingAction
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.UtStatementModel
import org.utbot.framework.plugin.api.classId
import org.utbot.framework.plugin.api.util.defaultValueModel
import org.utbot.framework.plugin.api.util.doubleArrayClassId
import org.utbot.framework.plugin.api.util.doubleClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.intArrayClassId
import org.utbot.framework.plugin.api.util.intClassId
import org.utbot.framework.plugin.api.util.isArray
import org.utbot.framework.plugin.api.util.isPrimitiveWrapper
import org.utbot.framework.plugin.api.util.longArrayClassId
import org.utbot.framework.plugin.api.util.longClassId
import org.utbot.framework.plugin.api.util.methodId
import org.utbot.framework.plugin.api.util.objectArrayClassId
import org.utbot.framework.plugin.api.util.objectClassId
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
            UT_STREAM -> java.util.stream.Stream::class.java.id
            UT_INT_STREAM -> java.util.stream.IntStream::class.java.id
            UT_LONG_STREAM -> java.util.stream.LongStream::class.java.id
            UT_DOUBLE_STREAM -> java.util.stream.DoubleStream::class.java.id
        }
}

abstract class StreamWrapper(
    utStreamClass: UtStreamClass, private val elementsClassId: ClassId
) : BaseGenericStorageBasedContainerWrapper(utStreamClass.className) {
    protected val streamClassId = utStreamClass.overriddenStreamClassId

    protected abstract val mapMethodId: MethodId
    protected abstract val filterMethodId: MethodId

    private val limitMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "limit",
        returnType = streamClassId,
        arguments = arrayOf(longClassId)
    )

    private val skipMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "map",
        returnType = streamClassId,
        arguments = arrayOf(longClassId)
    )

    protected abstract val peekMethodId: MethodId

    private val distinctMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "distinct",
        returnType = streamClassId,
        arguments = emptyArray()
    )

    private val naturalSortedMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "sorted",
        returnType = streamClassId,
        arguments = emptyArray()
    )

    protected abstract val sortedMethodId: MethodId

    override fun value(resolver: Resolver, wrapper: ObjectValue): UtAssembleModel = resolver.run {
        val addr = holder.concreteAddr(wrapper.addr)
        val modelName = nextModelName(baseModelName)

        val collectedFieldModels = collectFieldModels(wrapper.addr, overriddenClass.type)

        val isFromPrimitives = resolveIsFromPrimitives(collectedFieldModels).value as Boolean
        val actionModels = resolveActions(collectedFieldModels)
        val sourceModel = resolveSource(wrapper, collectedFieldModels).let {
            if (!isFromPrimitives) {
                it
            } else {
                // transform array model for wrappers to array model for primitives
                (it as UtArrayModel).transformElementsModel()
            }
        }

        val instantiationChain = mutableListOf<UtStatementModel>()
        val modificationsChain = emptyList<UtStatementModel>()

        UtAssembleModel(addr, streamClassId, modelName, instantiationChain, modificationsChain)
            .apply {
                instantiationChain += if (sourceModel is UtArrayModel) {
                    // stream was created from array or empty

                    val (executable, params) = if (sourceModel.length == 0) {
                        streamEmptyMethodId to emptyList()
                    } else {
                        streamOfMethodId to listOf(sourceModel)
                    }

                    UtExecutableCallModel(
                        instance = null,
                        executable = executable,
                        params = params,
                        returnValue = this
                    )
                } else {
                    UtExecutableCallModel(
                        instance = sourceModel,
                        executable = streamMethodId,
                        params = emptyList(),
                        returnValue = this
                    )

                }

                actionModels.forEach {
                    instantiationChain += UtExecutableCallModel(
                        instance = this,
                        executable = it.classId.getActionMethod(),
                        params = it.fields.values.toList(), // TODO few arguments, order?
                        returnValue = this
                    )
                }
            }
    }

    /**
     * Transforms model for array of wrappers (Integer, Long, Double, etc) to array of corresponding primitives.
     */
    private fun UtArrayModel.transformElementsModel(): UtArrayModel {
        return copy(
            classId = elementsClassId,
            constModel = constModel.wrapperModelToPrimitiveModel(),
            stores = stores.mapValuesTo(mutableMapOf()) { it.value.wrapperModelToPrimitiveModel() }
        )
    }

    private fun UtModel.wrapperModelToPrimitiveModel(): UtModel {
        if (this is UtNullModel) {
            return elementsClassId.elementClassId!!.defaultValueModel()
        }

        if (!classId.isPrimitiveWrapper || this !is UtAssembleModel) {
            return this
        }

        val firstChainStatement = allStatementsChain.firstOrNull() ?: return this
        val constructorCall = (firstChainStatement as? UtExecutableCallModel) ?: return this

        return (constructorCall.params.firstOrNull() as? UtPrimitiveModel) ?: this
    }

    override fun chooseClassIdWithConstructor(classId: ClassId): ClassId = error("No constructor for Stream")

    override val modificationMethodId: MethodId
        get() = error("No modification method for Stream")

    open fun Resolver.resolveSource(wrapper: ObjectValue, collectedFieldModels: Map<FieldId, UtModel>): UtReferenceModel {
        val originArrayField = FieldId(overriddenClass.type.classId, "originArray")

        val originArrayModel = collectedFieldModels[originArrayField]

        // stream was created from array or empty
        if (originArrayModel !is UtNullModel) {
            return originArrayModel as UtArrayModel
        }

        // stream was not created from array, resolve origin collection
        val originCollectionField = FieldId(overriddenClass.type.classId, "origin")

        val collectionModel = collectedFieldModels[originCollectionField]

        require(collectionModel is UtReferenceModel) {
            "Origin collection for Stream ${overriddenClass.type} wrapper was expected to be UtReferenceModel " +
                    "but $collectionModel found"
        }

        return collectionModel
    }

    private fun resolveActions(collectedFieldModels: Map<FieldId, UtModel>): List<UtCompositeModel> {
        val actionsFieldId = FieldId(overriddenClass.type.classId, "actions")
        val actionsArrayModel = collectedFieldModels[actionsFieldId] as? UtArrayModel ?: return emptyList()

        val actionModels = constructValues(actionsArrayModel, actionsArrayModel.length).flatten()

        return actionModels.filterIsInstance<UtCompositeModel>()
    }

    private fun resolveIsFromPrimitives(collectedFieldModels: Map<FieldId, UtModel>): UtPrimitiveModel {
        val isCreatedFromPrimitiveArrayField = FieldId(overriddenClass.type.classId, "isCreatedFromPrimitiveArray")

        return collectedFieldModels[isCreatedFromPrimitiveArrayField] as UtPrimitiveModel
    }

    private fun ClassId.getActionMethod(): MethodId =
        when (this) {
            ConsumerAction::class.id -> peekMethodId
            DistinctAction::class.id -> distinctMethodId
            FilterAction::class.id -> filterMethodId
            LimitAction::class.id -> limitMethodId
            MapAction::class.id -> mapMethodId
            NaturalSortingAction::class.id -> naturalSortedMethodId
            SkipAction::class.id -> skipMethodId
            SortingAction::class.id -> sortedMethodId
            else -> error("Unknown Stream action $this")
        }

    private val collectionId: ClassId = java.util.Collection::class.java.id

    private val streamMethodId: MethodId
        get() = methodId(
            classId = collectionId,
            name = "stream",
            returnType = streamClassId,
            arguments = emptyArray()
        )

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

    override val mapMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "map",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.Function::class.id)
    )

    override val filterMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "filter",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.Predicate::class.id)
    )

    override val peekMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "peek",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.Consumer::class.id)
    )

    override val sortedMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "sorted",
        returnType = streamClassId,
        arguments = arrayOf(java.util.Comparator::class.id)
    )
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
}

class IntStreamWrapper : PrimitiveStreamWrapper(UtStreamClass.UT_INT_STREAM, intArrayClassId) {
    override val baseModelName: String = "intStream"

    override val mapMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "map",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.IntFunction::class.id)
    )

    override val filterMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "filter",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.IntPredicate::class.id)
    )

    override val peekMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "peek",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.IntConsumer::class.id)
    )

    override val sortedMethodId: MethodId
        get() = error("Method sorted with custom comparator does not exist for $streamClassId")
}

class LongStreamWrapper : PrimitiveStreamWrapper(UtStreamClass.UT_LONG_STREAM, longArrayClassId) {
    override val baseModelName: String = "longStream"

    override val mapMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "map",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.LongFunction::class.id)
    )

    override val filterMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "filter",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.LongPredicate::class.id)
    )

    override val peekMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "peek",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.LongConsumer::class.id)
    )

    override val sortedMethodId: MethodId
        get() = error("Method sorted with custom comparator does not exist for $streamClassId")
}

class DoubleStreamWrapper : PrimitiveStreamWrapper(UtStreamClass.UT_DOUBLE_STREAM, doubleArrayClassId) {
    override val baseModelName: String = "doubleStream"

    override val mapMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "map",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.DoubleFunction::class.id)
    )

    override val filterMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "filter",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.DoublePredicate::class.id)
    )

    override val peekMethodId: MethodId = methodId(
        classId = streamClassId,
        name = "peek",
        returnType = streamClassId,
        arguments = arrayOf(java.util.function.DoubleConsumer::class.id)
    )

    override val sortedMethodId: MethodId
        get() = error("Method sorted with custom comparator does not exist for $streamClassId")
}
