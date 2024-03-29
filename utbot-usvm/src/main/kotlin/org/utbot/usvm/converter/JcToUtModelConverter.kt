package org.utbot.usvm.converter

import org.jacodb.api.JcClasspath
import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.api.UTestMock
import org.usvm.instrumentation.testcase.descriptor.UTestArrayDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestClassDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestConstantDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestCyclicReferenceDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestEnumValueDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestExceptionDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestObjectDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestRefDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestValueDescriptor
import org.usvm.instrumentation.util.InstrumentationModuleConstants.nameForExistingButNullString
import org.utbot.framework.fuzzer.IdGenerator
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtCustomModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.UtReferenceModel
import org.utbot.framework.plugin.api.mapper.UtModelDeepMapper
import org.utbot.framework.plugin.api.mapper.UtModelMapper
import org.utbot.framework.plugin.api.util.classClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.stringClassId

enum class EnvironmentStateKind {
    INITIAL, FINAL
}

data class UtCyclicReferenceModel(
    override val id: Int?,
    override val classId: ClassId,
    val refId: Int,
    val stateKind: EnvironmentStateKind,
) : UtCustomModel(id, classId) {
    override fun shallowMap(mapper: UtModelMapper): UtCustomModel = this
}

class JcToUtModelConverter(
    private val idGenerator: IdGenerator<Int>,
    private val instToUtModelConverter: UTestInstToUtModelConverter,
) {
    private val descriptorToModelCache =
        mutableMapOf<Pair<UTestValueDescriptor, EnvironmentStateKind>, UtModel>()
    private val refIdAndStateKindToDescriptorCache =
        mutableMapOf<Pair<Int, EnvironmentStateKind>, UTestValueDescriptor>()
    val utCyclicReferenceModelResolver = UtModelDeepMapper { model ->
        when (model) {
            is UtCyclicReferenceModel -> getModelByRefIdAndStateKind(model.refId, model.stateKind)
                ?: error("Invalid UTestCyclicReferenceDescriptor: $model")
            else -> model
        }
    }

    fun convert(
        valueDescriptor: UTestValueDescriptor,
        stateKind: EnvironmentStateKind,
    ): UtModel = descriptorToModelCache.getOrPut(valueDescriptor to stateKind) {
        if (stateKind == EnvironmentStateKind.INITIAL || valueDescriptor.origin is UTestMock)
            valueDescriptor.origin?.let { originExpr ->
                val model = instToUtModelConverter.findModelByInst(originExpr as UTestExpression)
                if (model is UtAssembleModel && model.origin == null) {
                    val compositeOrigin = convertIgnoringOriginExprForThisModel(
                        valueDescriptor = valueDescriptor,
                        stateKind = stateKind,
                        curModelId = model.id ?: idGenerator.createId(),
                    )
                    if (compositeOrigin is UtCompositeModel)
                        return@getOrPut model.copy(origin = compositeOrigin)
                }
                return@getOrPut model
            }

        val previousStateModel =
            if (stateKind == EnvironmentStateKind.FINAL && valueDescriptor is UTestRefDescriptor) {
                (getModelByRefIdAndStateKind(valueDescriptor.refId, EnvironmentStateKind.INITIAL) as? UtReferenceModel)
            } else {
                null
            }

        return@getOrPut convertIgnoringOriginExprForThisModel(
            valueDescriptor,
            stateKind,
            curModelId = previousStateModel?.id ?: idGenerator.createId()
        )
    }

    private fun convertIgnoringOriginExprForThisModel(
        valueDescriptor: UTestValueDescriptor,
        stateKind: EnvironmentStateKind,
        curModelId: Int,
    ): UtModel = descriptorToModelCache.getOrPut(valueDescriptor to stateKind) {
        if (valueDescriptor is UTestRefDescriptor) {
            refIdAndStateKindToDescriptorCache[valueDescriptor.refId to stateKind] = valueDescriptor
        }

        return when (valueDescriptor) {
            is UTestObjectDescriptor -> {
                val fields = mutableMapOf<FieldId, UtModel>()

                val model = UtCompositeModel(
                    id = curModelId,
                    classId = valueDescriptor.type.classId,
                    isMock = false,
                    mocks = mutableMapOf(),
                    fields = fields,
                )

                descriptorToModelCache[valueDescriptor to stateKind] = model

                fields += valueDescriptor.fields
                    .entries
                    .filter { (jcField, _) -> !jcField.isStatic }
                    .associate { (jcField, fieldDescr) ->
                        val fieldId = FieldId(jcField.enclosingClass.classId, jcField.name)
                        val fieldModel = convert(fieldDescr, stateKind)
                        fieldId to fieldModel
                    }

                model
            }

            is UTestArrayDescriptor -> {
                val stores = mutableMapOf<Int, UtModel>()

                val model = UtArrayModel(
                    id = curModelId,
                    classId = valueDescriptor.type.classId,
                    length = valueDescriptor.length,
                    constModel = UtNullModel(valueDescriptor.elementType.classId),
                    stores = stores,
                )

                descriptorToModelCache[valueDescriptor to stateKind] = model

                valueDescriptor.value
                    .map { elemDescr -> convert(elemDescr, stateKind) }
                    .forEachIndexed { index, elemModel -> stores += index to elemModel }

                model
            }

            is UTestClassDescriptor -> UtClassRefModel(
                id = curModelId,
                classId = classClassId,
                value = valueDescriptor.classType.classId,
            )

            is UTestConstantDescriptor.Null -> UtNullModel(valueDescriptor.type.classId)

            is UTestConstantDescriptor.Boolean -> UtPrimitiveModel(valueDescriptor.value)
            is UTestConstantDescriptor.Byte -> UtPrimitiveModel(valueDescriptor.value)
            is UTestConstantDescriptor.Char -> UtPrimitiveModel(valueDescriptor.value)
            is UTestConstantDescriptor.Double -> UtPrimitiveModel(valueDescriptor.value)
            is UTestConstantDescriptor.Float -> UtPrimitiveModel(valueDescriptor.value)
            is UTestConstantDescriptor.Int -> UtPrimitiveModel(valueDescriptor.value)
            is UTestConstantDescriptor.Long -> UtPrimitiveModel(valueDescriptor.value)
            is UTestConstantDescriptor.Short -> UtPrimitiveModel(valueDescriptor.value)
            is UTestConstantDescriptor.String -> constructString(valueDescriptor.value)

            is UTestCyclicReferenceDescriptor -> getModelByRefIdAndStateKind(valueDescriptor.refId, stateKind)
                ?: UtCyclicReferenceModel(
                    id = curModelId,
                    classId = valueDescriptor.type.classId,
                    refId = valueDescriptor.refId,
                    stateKind = stateKind
                )

            is UTestEnumValueDescriptor -> UtEnumConstantModel(
                id = curModelId,
                classId = valueDescriptor.type.classId,
                value = valueDescriptor.type.classId.jClass.enumConstants.find {
                    // [valueDescriptor.enumValueName] is the enum value to which toString() was applied
                    (it as Enum<*>).toString() == valueDescriptor.enumValueName
                } as Enum<*>
            )
            is UTestExceptionDescriptor -> UtCompositeModel(
                id = curModelId,
                classId = valueDescriptor.type.classId,
                isMock = false,
                fields = mutableMapOf(
                    // TODO usvm-sbft: ask why `UTestExceptionDescriptor.message` is not nullable, support it here
                    FieldId(Throwable::class.java.id, "detailMessage") to UtPrimitiveModel(valueDescriptor.message)
                )
            )
        }
    }

    private fun constructString(valueDescriptorValue: String): UtModel {
        if(valueDescriptorValue == nameForExistingButNullString){
            return UtNullModel(stringClassId)
        }
        return UtPrimitiveModel(valueDescriptorValue)
    }

    private fun getModelByRefIdAndStateKind(
        refId: Int,
        stateKind: EnvironmentStateKind
    ): UtModel? =
        refIdAndStateKindToDescriptorCache[refId to stateKind]?.let {
            descriptorToModelCache[it to stateKind]
        }
}