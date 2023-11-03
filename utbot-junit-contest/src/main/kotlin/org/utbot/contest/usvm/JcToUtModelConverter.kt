package org.utbot.contest.usvm

import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.descriptor.Descriptor2ValueConverter
import org.usvm.instrumentation.testcase.descriptor.UTestArrayDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestClassDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestConstantDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestCyclicReferenceDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestEnumValueDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestExceptionDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestObjectDescriptor
import org.usvm.instrumentation.testcase.descriptor.UTestValueDescriptor
import org.usvm.instrumentation.util.toJavaClass
import org.utbot.framework.codegen.domain.builtin.UtilMethodProvider
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtCustomModel
import org.utbot.framework.plugin.api.UtEnumConstantModel
import org.utbot.framework.plugin.api.UtLambdaModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.mapper.UtModelDeepMapper
import org.utbot.framework.plugin.api.mapper.map
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.fuzzer.IdGenerator
import org.utbot.instrumentation.instrumentation.execution.constructors.StateBeforeAwareIdGenerator
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructor
import org.utbot.instrumentation.instrumentation.execution.constructors.javaStdLibModelWithCompositeOriginConstructors
import java.util.*

class JcToUtModelConverter(
    private val idGenerator: IdGenerator<Int>,
    private val instToModelCache: Map<UTestExpression, UtModel>,
) {

    private val classLoader = utContext.classLoader
    private val toValueConverter = Descriptor2ValueConverter(classLoader)

    private val utModelConstructor = UtModelConstructor(
        objectToModelCache = IdentityHashMap(),
        idGenerator = StateBeforeAwareIdGenerator(allPreExistingModels = emptySet()),
        utModelWithCompositeOriginConstructorFinder = { classId ->
            javaStdLibModelWithCompositeOriginConstructors[classId.jClass]?.invoke()
        }
    )

    private val descriptorToModelCache = mutableMapOf<UTestValueDescriptor, UtModel>()

    fun convert(valueDescriptor: UTestValueDescriptor?): UtModel {
        valueDescriptor?.origin?.let {
            return instToModelCache.getValue(it as UTestExpression)
        }

        // TODO usvm-sbft: ask why `UTestExecutionSuccessResult.result` is nullable
        if (valueDescriptor == null) {
            return UtNullModel(objectClassId)
        }

        when (valueDescriptor) {
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
                // add fields
            }

            is UTestArrayDescriptor.Array -> TODO()
            is UTestArrayDescriptor.BooleanArray -> TODO()
            is UTestArrayDescriptor.ByteArray -> TODO()
            is UTestArrayDescriptor.CharArray -> TODO()
            is UTestArrayDescriptor.DoubleArray -> TODO()
            is UTestArrayDescriptor.FloatArray -> TODO()
            is UTestArrayDescriptor.IntArray -> TODO()
            is UTestArrayDescriptor.LongArray -> TODO()
            is UTestArrayDescriptor.ShortArray -> TODO()
            is UTestClassDescriptor -> TODO()
            is UTestConstantDescriptor.Boolean -> TODO()
            is UTestConstantDescriptor.Byte -> TODO()
            is UTestConstantDescriptor.Char -> TODO()
            is UTestConstantDescriptor.Double -> TODO()
            is UTestConstantDescriptor.Float -> TODO()
            is UTestConstantDescriptor.Int -> TODO()
            is UTestConstantDescriptor.Long -> TODO()
            is UTestConstantDescriptor.Null -> TODO()
            is UTestConstantDescriptor.Short -> TODO()
            is UTestConstantDescriptor.String -> TODO()
            is UTestCyclicReferenceDescriptor -> TODO()
            is UTestEnumValueDescriptor -> TODO()
            is UTestExceptionDescriptor -> TODO()
        }

        val concreteValue = toValueConverter.buildObjectFromDescriptor(valueDescriptor)
        val objectType = valueDescriptor.type.toJavaClass(classLoader).id

        return utModelConstructor.construct(concreteValue, objectType)
    }
}