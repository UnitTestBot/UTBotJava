package org.utbot.contest.usvm

import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.descriptor.UTestArrayDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestClassDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestConstantDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestCyclicReferenceDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestEnumValueDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestExceptionDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestObjectDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestValueDescriptor
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.classClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.fuzzer.IdGenerator

class JcToUtModelConverter(
    private val idGenerator: IdGenerator<Int>,
    private val instToModelCache: Map<UTestExpression, UtModel>,
) {
    private val descriptorToModelCache = mutableMapOf<UTestValueDescriptor, UtModel>()

    fun convert(valueDescriptor: UTestValueDescriptor?): UtModel {
        valueDescriptor?.origin?.let {
            return instToModelCache.getValue(it as UTestExpression)
        }

        // TODO usvm-sbft: ask why `UTestExecutionSuccessResult.result` is nullable
        if (valueDescriptor == null) {
            return UtNullModel(objectClassId)
        }

        return when (valueDescriptor) {
            is UTestObjectDescriptor -> {
                val fields = mutableMapOf<FieldId, UtModel>()

                val model = UtCompositeModel(
                    id = idGenerator.createId(),
                    classId = valueDescriptor.type.classId,
                    isMock = false,
                    mocks = mutableMapOf(),
                    fields = fields,
                )

                descriptorToModelCache[valueDescriptor] = model

                fields += valueDescriptor.fields
                    .entries
                    .associate { (jcField, fieldDescr) ->
                        val fieldId = FieldId(jcField.type.classId, jcField.name)
                        val fieldModel = convert(fieldDescr)
                        fieldId to fieldModel
                    }

                model
            }

            is UTestArrayDescriptor.Array -> {
                val stores = mutableMapOf<Int, UtModel>()

                val model = UtArrayModel(
                    id = idGenerator.createId(),
                    classId = valueDescriptor.type.classId,
                    length = valueDescriptor.length,
                    constModel = UtNullModel(valueDescriptor.elementType.classId),
                    stores = stores,
                )

                descriptorToModelCache[valueDescriptor] = model

                valueDescriptor.value
                    .map { elemDescr -> convert(elemDescr) }
                    .forEachIndexed { index, elemModel -> stores += index to elemModel }

                model
            }

            is UTestArrayDescriptor.BooleanArray -> constructArrayModel(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.ByteArray -> constructArrayModel(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.CharArray -> constructArrayModel(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.DoubleArray -> constructArrayModel(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.FloatArray -> constructArrayModel(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.IntArray -> constructArrayModel(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.LongArray -> constructArrayModel(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.ShortArray -> constructArrayModel(valueDescriptor, valueDescriptor.value.toList())

            is UTestClassDescriptor -> UtClassRefModel(
                id = idGenerator.createId(),
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
            is UTestConstantDescriptor.String -> UtPrimitiveModel(valueDescriptor.value)

            is UTestCyclicReferenceDescriptor -> TODO()
            is UTestEnumValueDescriptor -> TODO()
            is UTestExceptionDescriptor -> TODO()
        }
    }

    private fun constructArrayModel(valueDescriptor: UTestArrayDescriptor<*>, arrayContent: List<Any>): UtArrayModel {
        val stores = mutableMapOf<Int, UtModel>()

        val model = UtArrayModel(
            id = idGenerator.createId(),
            classId = valueDescriptor.type.classId,
            length = valueDescriptor.length,
            constModel = UtNullModel(valueDescriptor.elementType.classId),
            stores = stores,
        )

        descriptorToModelCache[valueDescriptor] = model

        arrayContent
            .map { elemValue -> UtPrimitiveModel(elemValue) }
            .forEachIndexed { index, elemModel -> stores += index to elemModel }

        return model
    }

}