package org.utbot.contest.usvm

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
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.instrumentation.instrumentation.execution.constructors.StateBeforeAwareIdGenerator
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructor
import org.utbot.instrumentation.instrumentation.execution.constructors.javaStdLibModelWithCompositeOriginConstructors
import java.util.*

class JcToUtModelConverter(utilMethodProvider: UtilMethodProvider) {

    private val classLoader = utContext.classLoader
    private val toValueConverter = Descriptor2ValueConverter(classLoader)
    private val inst2modelConverter = UTestInst2UtModelConverter(utilMethodProvider)


    private val utModelConstructor = UtModelConstructor(
        objectToModelCache = IdentityHashMap(),
        idGenerator = StateBeforeAwareIdGenerator(allPreExistingModels = emptySet()),
        utModelWithCompositeOriginConstructorFinder = { classId ->
            javaStdLibModelWithCompositeOriginConstructors[classId.jClass]?.invoke()
        }
    )

    private val descrToModelCache = mutableMapOf<UTestValueDescriptor, UtModel>()

    fun convert(valueDescriptor: UTestValueDescriptor?): UtModel {
        //WTF, how can it happen? but null can be passed here
        if (valueDescriptor == null) {
            return UtNullModel(objectClassId)
        }

        return descrToModelCache.getOrPut(valueDescriptor) {
            val concreteValue = toValueConverter.buildObjectFromDescriptor(valueDescriptor)
            val objectType = valueDescriptor.type.toJavaClass(classLoader).id

            val missingMocksModel = utModelConstructor.construct(concreteValue, objectType)

            return when (missingMocksModel) {
                is UtNullModel,
                is UtPrimitiveModel,
                is UtClassRefModel,
                is UtEnumConstantModel,
                is UtLambdaModel -> missingMocksModel
                is UtCompositeModel,
                is UtArrayModel,
                is UtAssembleModel -> {
                    valueDescriptor.origin
                    ?.let { inst2modelConverter.convert(it) }
                        ?: missingMocksModel
                }
                is UtCustomModel -> error("Custom models are not supported in Contest")
                else -> error("The type of $missingMocksModel is not supported in Contest")
            }
        }
    }
}