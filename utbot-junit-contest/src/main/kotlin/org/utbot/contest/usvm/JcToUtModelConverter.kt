package org.utbot.contest.usvm

import org.usvm.instrumentation.classloader.WorkerClassLoader
import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.api.UTestMock
import org.usvm.instrumentation.testcase.api.UTestMockObject
import org.usvm.instrumentation.testcase.descriptor.Descriptor2ValueConverter
import org.usvm.instrumentation.testcase.descriptor.UTestValueDescriptor
import org.usvm.instrumentation.testcase.descriptor.Value2DescriptorConverter
import org.usvm.instrumentation.testcase.executor.UTestExpressionExecutor
import org.usvm.instrumentation.util.toJavaClass
import org.usvm.instrumentation.util.toJavaField
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.instrumentation.instrumentation.execution.constructors.StateBeforeAwareIdGenerator
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructor
import org.utbot.instrumentation.instrumentation.execution.constructors.javaStdLibModelWithCompositeOriginConstructors
import java.util.*

class JcToUtModelConverter {

    private val classLoader = utContext.classLoader
    private val toValueConverter = Descriptor2ValueConverter(classLoader)

    // TODO: properly deal with test executor and related features
    private val testExecutor = UTestExpressionExecutor()
    private val toDescriptorConverter = Value2DescriptorConverter(WorkerClassLoader(), null)


    private val utModelConstructor = UtModelConstructor(
        objectToModelCache = IdentityHashMap(),
        idGenerator = StateBeforeAwareIdGenerator(allPreExistingModels = emptySet()),
        utModelWithCompositeOriginConstructorFinder = { classId ->
            javaStdLibModelWithCompositeOriginConstructors[classId.jClass]?.invoke()
        }
    )

    private val exprToModelCache = mutableMapOf<UTestMock, UtCompositeModel>()
    private val descrToModelCache = mutableMapOf<UTestValueDescriptor, UtModel>()

    fun convert(valueDescriptor: UTestValueDescriptor?): UtModel {
        //WTF, how can it happen? but null can be passed here
        if (valueDescriptor == null) {
            return UtNullModel(objectClassId)
        }

        return descrToModelCache.getOrPut(valueDescriptor) {
            val concreteValue = toValueConverter.buildObjectFromDescriptor(valueDescriptor)
            val objectType = valueDescriptor.type.toJavaClass(classLoader).id

            val mocklessModel = utModelConstructor.construct(concreteValue, objectType)

            //TODO: think about assemble models, arrays, with inner mocks; may be enums
            if (mocklessModel !is UtCompositeModel) {
                return mocklessModel
            }

            val instantiatingExpr: UTestMock = // TODO: valueDescriptor.instantiatingExpr
                UTestMockObject(valueDescriptor.type, emptyMap(), emptyMap())

            restoreMockInfo(instantiatingExpr, mocklessModel)
        }
    }


    private fun restoreMockInfo(
        mockExpr: UTestMock,
        mocklessModel: UtCompositeModel,
    ): UtModel {

        exprToModelCache[mockExpr]?.let { return it }

        val fields = mutableMapOf<FieldId, UtModel>()
        val mocks = mutableMapOf<ExecutableId, List<UtModel>>()

        val finalModel = UtCompositeModel(
            id = mocklessModel.id,
            classId = mocklessModel.classId,
            isMock = true,
            fields = fields,
            mocks = mocks,
        )
        exprToModelCache[mockExpr] = finalModel

        fields += mockExpr.fields
            .entries
            .associate { (jcField, uTestExpr) ->
                val fieldType = jcField.toJavaField(classLoader)!!.fieldId.type
                val fieldId = FieldId(fieldType, jcField.name)

                val fieldModelDescr = exprToDescriptor(uTestExpr)

                val fieldModel = convert(fieldModelDescr)
                fieldId to fieldModel
            }

        mocks += mockExpr.methods
            .entries
            .associate { (jcMethod, uTestExprs) ->
                val executableId: ExecutableId = jcMethod.toExecutableId()

                val models = uTestExprs
                    .map { expr -> exprToDescriptor(expr) }
                    .map { descr -> convert(descr) }

                executableId to models
            }

        return finalModel
    }

    private fun exprToDescriptor(expr: UTestExpression): UTestValueDescriptor =
        toDescriptorConverter
            .buildDescriptorFromUTestExpr(expr, testExecutor)
            ?.getOrNull()!!
            .valueDescriptor!!
}