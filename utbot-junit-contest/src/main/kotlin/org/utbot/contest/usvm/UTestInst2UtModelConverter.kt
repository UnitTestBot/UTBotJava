package org.utbot.contest.usvm

import org.usvm.instrumentation.testcase.UTest
import org.usvm.instrumentation.testcase.api.UTestAllocateMemoryCall
import org.usvm.instrumentation.testcase.api.UTestArithmeticExpression
import org.usvm.instrumentation.testcase.api.UTestArrayGetExpression
import org.usvm.instrumentation.testcase.api.UTestArrayLengthExpression
import org.usvm.instrumentation.testcase.api.UTestArraySetStatement
import org.usvm.instrumentation.testcase.api.UTestBinaryConditionExpression
import org.usvm.instrumentation.testcase.api.UTestBinaryConditionStatement
import org.usvm.instrumentation.testcase.api.UTestBooleanExpression
import org.usvm.instrumentation.testcase.api.UTestByteExpression
import org.usvm.instrumentation.testcase.api.UTestCastExpression
import org.usvm.instrumentation.testcase.api.UTestCharExpression
import org.usvm.instrumentation.testcase.api.UTestClassExpression
import org.usvm.instrumentation.testcase.api.UTestConstructorCall
import org.usvm.instrumentation.testcase.api.UTestCreateArrayExpression
import org.usvm.instrumentation.testcase.api.UTestDoubleExpression
import org.usvm.instrumentation.testcase.api.UTestExpression
import org.usvm.instrumentation.testcase.api.UTestFloatExpression
import org.usvm.instrumentation.testcase.api.UTestGetFieldExpression
import org.usvm.instrumentation.testcase.api.UTestGetStaticFieldExpression
import org.usvm.instrumentation.testcase.api.UTestGlobalMock
import org.usvm.instrumentation.testcase.api.UTestInst
import org.usvm.instrumentation.testcase.api.UTestIntExpression
import org.usvm.instrumentation.testcase.api.UTestLongExpression
import org.usvm.instrumentation.testcase.api.UTestMethodCall
import org.usvm.instrumentation.testcase.api.UTestMockObject
import org.usvm.instrumentation.testcase.api.UTestNullExpression
import org.usvm.instrumentation.testcase.api.UTestSetFieldStatement
import org.usvm.instrumentation.testcase.api.UTestSetStaticFieldStatement
import org.usvm.instrumentation.testcase.api.UTestShortExpression
import org.usvm.instrumentation.testcase.api.UTestStaticMethodCall
import org.usvm.instrumentation.testcase.api.UTestStringExpression
import org.utbot.framework.codegen.domain.builtin.UtilMethodProvider
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.UtArrayModel
import org.utbot.framework.plugin.api.UtAssembleModel
import org.utbot.framework.plugin.api.UtClassRefModel
import org.utbot.framework.plugin.api.UtCompositeModel
import org.utbot.framework.plugin.api.UtExecutableCallModel
import org.utbot.framework.plugin.api.UtInstrumentation
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.classClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.fuzzer.IdGenerator

data class UTestProcessResult(
    val exprToModelCache: Map<UTestExpression, UtModel>,
    val instantiationCallToAssembleModelCache: Map<UtExecutableCallModel, UtAssembleModel>,
    val instrumentations: List<UtInstrumentation>,
    )

class UTestInst2UtModelConverter(
    private val idGenerator: IdGenerator<Int>,
    private val utilMethodProvider: UtilMethodProvider,
) {
    private val exprToModelCache = mutableMapOf<UTestExpression, UtModel>()
    private val instantiationCallToAssembleModelCache = mutableMapOf<UtExecutableCallModel, UtAssembleModel>()
    private val instrumentations = mutableListOf<UtInstrumentation>()

    fun processUTest(uTest: UTest): UTestProcessResult {
        exprToModelCache.clear()
        instrumentations.clear()

        uTest.initStatements.forEach { uInst -> processInst(uInst) }
        processInst(uTest.callMethodExpression)

        return UTestProcessResult(exprToModelCache, instantiationCallToAssembleModelCache, instrumentations)
    }

    private fun processInst(uTestInst: UTestInst) {
        when (uTestInst) {
            is UTestExpression -> processExpr(uTestInst)

            is UTestArraySetStatement -> {
                val arrayModel = processExpr(uTestInst.arrayInstance)
                require(arrayModel is UtArrayModel)

                require(uTestInst.index is UTestIntExpression)
                val storeIndex = uTestInst.index as UTestIntExpression

                val elementModel = processExpr(uTestInst.setValueExpression)

                arrayModel.stores[storeIndex.value] = elementModel
            }

            is UTestSetFieldStatement -> {
                val instanceExpr = uTestInst.instance

                val instanceModel = processExpr(instanceExpr)
                require(instanceModel is UtAssembleModel)

                val fieldType = uTestInst.field.type.classId
                val fieldName = uTestInst.field.name
                val setValueModel = processExpr(uTestInst.value)

                val methodCall = UtExecutableCallModel(
                    instance = null,
                    executable = utilMethodProvider.setFieldMethodId,
                    params = listOf(
                        instanceModel,
                        UtPrimitiveModel(fieldType.name),
                        UtPrimitiveModel(fieldName),
                        setValueModel,
                    ),
                )

                instanceModel?.let {
                    (it.modificationsChain as MutableList).add(methodCall)
                }
            }

            is UTestSetStaticFieldStatement -> processExpr(uTestInst.value)

            is UTestBinaryConditionStatement -> error("This expression type is not supported")
        }
    }

    private fun processExpr(uTestExpr: UTestExpression): UtModel = exprToModelCache.getOrPut(uTestExpr) {
        when (uTestExpr) {
            is UTestAllocateMemoryCall -> {
                val createInstanceCall = UtExecutableCallModel(
                    instance = null,
                    executable = utilMethodProvider.createInstanceMethodId,
                    params = listOf(UtPrimitiveModel(uTestExpr.clazz.classId.name)),
                )

                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.clazz.classId,
                    modelName = "",
                    instantiationCall = createInstanceCall,
                )

                instantiationCallToAssembleModelCache[createInstanceCall] = newModel

                newModel
            }

            is UTestConstructorCall -> {
                val constructorCall = UtExecutableCallModel(
                    instance = null,
                    executable = uTestExpr.method.toExecutableId(),
                    params = uTestExpr.args.map { arg ->
                        processExpr(arg)
                    },
                )

                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.type.classId,
                    modelName = "",
                    instantiationCall = constructorCall,
                )

                instantiationCallToAssembleModelCache[constructorCall] = newModel

                newModel
            }

            is UTestMethodCall -> {
                val instanceModel = processExpr(uTestExpr.instance)
                require(instanceModel is UtAssembleModel)

                val methodCall = UtExecutableCallModel(
                    instance = instanceModel,
                    executable = uTestExpr.method.toExecutableId(),
                    params = uTestExpr.args.map { arg ->
                        processExpr(arg)
                    },
                )

                (instanceModel.modificationsChain as MutableList).add(methodCall)

                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.type.classId,
                    modelName = "",
                    instantiationCall = methodCall,
                )

                instantiationCallToAssembleModelCache[methodCall] = newModel

                newModel
            }

            is UTestClassExpression -> UtClassRefModel(
                id = idGenerator.createId(),
                classId = classClassId,
                value = uTestExpr.type.classId,
            )


            is UTestBooleanExpression -> UtPrimitiveModel(uTestExpr.value)
            is UTestByteExpression -> UtPrimitiveModel(uTestExpr.value)
            is UTestCharExpression -> UtPrimitiveModel(uTestExpr.value)
            is UTestDoubleExpression -> UtPrimitiveModel(uTestExpr.value)
            is UTestFloatExpression -> UtPrimitiveModel(uTestExpr.value)
            is UTestIntExpression -> UtPrimitiveModel(uTestExpr.value)
            is UTestLongExpression -> UtPrimitiveModel(uTestExpr.value)
            is UTestShortExpression -> UtPrimitiveModel(uTestExpr.value)
            is UTestStringExpression -> UtPrimitiveModel(uTestExpr.value)

            is UTestNullExpression -> UtNullModel(uTestExpr.type.classId)

            is UTestCreateArrayExpression -> {
                require(uTestExpr.size is UTestIntExpression)
                val arrayLength = uTestExpr.size as UTestIntExpression

                UtArrayModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.type.classId,
                    length = arrayLength.value,
                    constModel = UtNullModel(objectClassId),
                    stores = mutableMapOf(),
                )
            }

            is UTestGetFieldExpression -> {
                val instanceModel = processExpr(uTestExpr.instance)

                val getFieldCall = UtExecutableCallModel(
                    instance = null,
                    executable = utilMethodProvider.getFieldValueMethodId,
                    params = listOf(
                        instanceModel,
                        UtPrimitiveModel(uTestExpr.field.type.classId.name),
                        UtPrimitiveModel(uTestExpr.field.name),
                    ),
                )

                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.type.classId,
                    modelName = "",
                    instantiationCall = getFieldCall,
                )

                instantiationCallToAssembleModelCache[getFieldCall] = newModel

                newModel
            }

            is UTestGetStaticFieldExpression -> {
                val getStaticFieldCall = UtExecutableCallModel(
                    instance = null,
                    executable = utilMethodProvider.getStaticFieldValueMethodId,
                    params = listOf(
                        UtPrimitiveModel(uTestExpr.field.type.classId.name),
                        UtPrimitiveModel(uTestExpr.field.name),
                    ),
                )

                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.type.classId,
                    modelName = "",
                    instantiationCall = getStaticFieldCall,
                )

                instantiationCallToAssembleModelCache[getStaticFieldCall] = newModel

                newModel
            }

            is UTestMockObject -> {
                val fields = mutableMapOf<FieldId, UtModel>()
                val mocks = mutableMapOf<ExecutableId, List<UtModel>>()

                val newModel = UtCompositeModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.type.classId,
                    isMock = true,
                    fields = fields,
                    mocks = mocks,
                )
                exprToModelCache[uTestExpr] = newModel

                fields += uTestExpr.fields
                    .entries
                    .associate { (jcField, uTestExpr) ->
                        jcField.fieldId to processExpr(uTestExpr)
                    }

                mocks += uTestExpr.methods
                    .entries
                    .associate { (jcMethod, uTestExprs) ->
                        val executableId: ExecutableId = jcMethod.toExecutableId()
                        val models = uTestExprs.map { expr ->
                            processExpr(expr)
                        }

                        executableId to models
                    }

                newModel
            }

            is UTestGlobalMock -> {
                // TODO usvm-sbft: collect instrumentations here
                UtClassRefModel(
                    id = idGenerator.createId(),
                    classId = classClassId,
                    value = uTestExpr.type.classId
                )
            }

            is UTestArithmeticExpression -> error("This expression type is not supported")
            is UTestBinaryConditionExpression -> error("This expression type is not supported")

            is UTestStaticMethodCall -> error("This expression type is not supported")

            is UTestCastExpression -> error("This expression type is not supported")

            is UTestArrayGetExpression -> error("This expression type is not supported")
            is UTestArrayLengthExpression -> error("This expression type is not supported")
        }
    }
}