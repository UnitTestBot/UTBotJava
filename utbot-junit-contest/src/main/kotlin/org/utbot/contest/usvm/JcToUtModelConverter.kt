package org.utbot.contest.usvm

import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.descriptor.Descriptor2ValueConverter
import org.usvm.instrumentation.testcase.descriptor.UTestValueDescriptor
import org.usvm.instrumentation.util.toJavaClass
import org.utbot.framework.codegen.domain.builtin.UtilMethodProvider
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
import org.utbot.instrumentation.instrumentation.execution.constructors.StateBeforeAwareIdGenerator
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructor
import org.utbot.instrumentation.instrumentation.execution.constructors.javaStdLibModelWithCompositeOriginConstructors
import java.util.*

class JcToUtModelConverter(
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

    fun convert(valueDescriptor: UTestValueDescriptor?): UtModel {
        // TODO usvm-sbft: ask why `UTestExecutionSuccessResult.result` is nullable
        if (valueDescriptor == null) {
            return UtNullModel(objectClassId)
        }

        val concreteValue = toValueConverter.buildObjectFromDescriptor(valueDescriptor)
        val objectType = valueDescriptor.type.toJavaClass(classLoader).id

        return utModelConstructor.construct(concreteValue, objectType)
    }
}