package org.utbot.contest.usvm

import org.jacodb.api.JcClasspath
import org.usvm.instrumentation.testcase.api.UTestExpression
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
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.classClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.stringClassId
import org.utbot.fuzzer.IdGenerator
import java.lang.Throwable

class JcToUtModelConverter(
    private val idGenerator: IdGenerator<Int>,
    private val jcClasspath: JcClasspath,
    private val instToUtModelConverter: UTestInst2UtModelConverter,
) {
    private val descriptorToModelCache = mutableMapOf<UTestValueDescriptor, UtModel>()
    private val refIdToDescriptorCache = mutableMapOf<Int, UTestValueDescriptor>()

    fun convert(valueDescriptor: UTestValueDescriptor): UtModel = descriptorToModelCache.getOrPut(valueDescriptor) {
        valueDescriptor.origin?.let { originExpr ->
            return instToUtModelConverter.findModelByInst(originExpr as UTestExpression)
        }

        if (valueDescriptor is UTestRefDescriptor)
            refIdToDescriptorCache[valueDescriptor.refId] = valueDescriptor

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
                        val fieldId = FieldId(jcField.enclosingClass.classId, jcField.name)
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

            is UTestArrayDescriptor.BooleanArray -> constructPrimitiveArray(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.ByteArray -> constructPrimitiveArray(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.CharArray -> constructPrimitiveArray(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.DoubleArray -> constructPrimitiveArray(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.FloatArray -> constructPrimitiveArray(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.IntArray -> constructPrimitiveArray(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.LongArray -> constructPrimitiveArray(valueDescriptor, valueDescriptor.value.toList())
            is UTestArrayDescriptor.ShortArray -> constructPrimitiveArray(valueDescriptor, valueDescriptor.value.toList())

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
            is UTestConstantDescriptor.String -> constructString(valueDescriptor.value)

            is UTestCyclicReferenceDescriptor -> descriptorToModelCache.getValue(
                refIdToDescriptorCache.getValue(valueDescriptor.refId)
            )
            is UTestEnumValueDescriptor -> UtEnumConstantModel(
                id = idGenerator.createId(),
                classId = valueDescriptor.type.classId,
                value = valueDescriptor.type.classId.jClass.enumConstants.find {
                    (it as Enum<*>).name == valueDescriptor.enumValueName
                } as Enum<*>
            )
            is UTestExceptionDescriptor -> UtCompositeModel(
                id = idGenerator.createId(),
                classId = valueDescriptor.type.classId,
                isMock = false,
                fields = mutableMapOf(
                    // TODO usvm-sbft: ask why `UTestExceptionDescriptor.message` is not nullable, support it here
                    FieldId(Throwable::class.java.id, "detailMessage") to UtPrimitiveModel(valueDescriptor.message)
                )
            )
        }
    }

    private fun constructPrimitiveArray(valueDescriptor: UTestArrayDescriptor<*>, arrayContent: List<Any>): UtArrayModel {
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

    private fun constructString(valueDescriptorValue: String): UtModel {
        if(valueDescriptorValue == nameForExistingButNullString){
            return UtNullModel(stringClassId)
        }
        return UtPrimitiveModel(valueDescriptorValue)
    }

}