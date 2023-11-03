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
import org.utbot.framework.plugin.api.UtNewInstanceInstrumentation
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.classClassId
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.fuzzer.IdGenerator
import org.utbot.fuzzer.ReferencePreservingIntIdGenerator

data class UTestInstOutput(
    val exprToModelCache: Map<UTestExpression, UtModel>,
    val instrumentations: List<UtInstrumentation>,
    )

class UTestInst2UtModelConverter(
    private val idGenerator: IdGenerator<Int>,
    private val utilMethodProvider: UtilMethodProvider,
) {
    private val exprToModelCache = mutableMapOf<UTestExpression, UtModel>()
    private val instrumentations = mutableListOf<UtInstrumentation>()

    fun processUTest(uTest: UTest): UTestInstOutput {
        exprToModelCache.clear()
        instrumentations.clear()

        uTest.initStatements.forEach { uInst -> processInst(uInst) }

        return UTestInstOutput(exprToModelCache, instrumentations)
    }

    private fun processInst(uTestInst: UTestInst) {
        exprToModelCache[uTestInst]?.let { return }

        when (uTestInst) {
            is UTestAllocateMemoryCall -> {
                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestInst.clazz.classId,
                    modelName = "",
                    instantiationCall = UtExecutableCallModel(
                        instance = null,
                        executable = utilMethodProvider.createInstanceMethodId,
                        params = listOf(UtPrimitiveModel(uTestInst.clazz.classId.name)),
                    ),
                )
                exprToModelCache[uTestInst] = newModel
            }

            is UTestConstructorCall -> {
                val constructorCall = UtExecutableCallModel(
                    instance = null,
                    executable = uTestInst.method.toExecutableId(),
                    params = uTestInst.args.map { arg ->
                        processInst(arg)
                        exprToModelCache[arg] ?: error("UtModel for $arg should have also been created")
                    },
                )

                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestInst.type.classId,
                    modelName = "",
                    instantiationCall = constructorCall,
                )

                exprToModelCache[uTestInst] = newModel
            }

            is UTestMethodCall -> {
                val instanceExpr = uTestInst.instance
                instanceExpr?.let { processInst(it) }

                val instanceModel = exprToModelCache[instanceExpr]
                    ?: error("UtModel for $instanceExpr should have also been created")
                require(instanceModel is UtAssembleModel)

                val methodCall = UtExecutableCallModel(
                    instance = instanceModel,
                    executable = uTestInst.method.toExecutableId(),
                    params = uTestInst.args.map { arg ->
                        processInst(arg)
                        exprToModelCache[arg] ?: error("UtModel for $arg should have also been created")
                    },
                )

                instanceModel?.let {
                    (it.modificationsChain as MutableList).add(methodCall)
                }

                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestInst.type.classId,
                    modelName = "",
                    instantiationCall = methodCall,
                )

                exprToModelCache[uTestInst] = newModel
            }

            is UTestClassExpression -> UtClassRefModel(
                    id = idGenerator.createId(),
                    classId = classClassId,
                    value = uTestInst.type.classId,
                )


            is UTestBooleanExpression -> UtPrimitiveModel(uTestInst.value)
            is UTestByteExpression -> UtPrimitiveModel(uTestInst.value)
            is UTestCharExpression -> UtPrimitiveModel(uTestInst.value)
            is UTestDoubleExpression -> UtPrimitiveModel(uTestInst.value)
            is UTestFloatExpression -> UtPrimitiveModel(uTestInst.value)
            is UTestIntExpression -> UtPrimitiveModel(uTestInst.value)
            is UTestLongExpression -> UtPrimitiveModel(uTestInst.value)
            is UTestShortExpression -> UtPrimitiveModel(uTestInst.value)
            is UTestStringExpression -> UtPrimitiveModel(uTestInst.value)

            is UTestNullExpression -> UtNullModel(uTestInst.type.classId)

            is UTestCreateArrayExpression -> {
                require(uTestInst.size is UTestIntExpression)
                val arrayLength = uTestInst.size as UTestIntExpression

                val newModel = UtArrayModel(
                    id = idGenerator.createId(),
                    classId = uTestInst.type.classId,
                    length = arrayLength.value,
                    constModel = UtNullModel(objectClassId),
                    stores = mutableMapOf(),
                )

                exprToModelCache[uTestInst] = newModel
            }

            is UTestArraySetStatement -> {
                val arrayModel = exprToModelCache[uTestInst.arrayInstance]
                requireNotNull(arrayModel)
                require(arrayModel is UtArrayModel)

                require(uTestInst.index is UTestIntExpression)
                val storeIndex = uTestInst.index as UTestIntExpression

                val setValueExpression = uTestInst.setValueExpression
                processInst(setValueExpression)
                val elementModel = exprToModelCache[setValueExpression]
                    ?: error("UtModel for $setValueExpression should have also been created")

                arrayModel.stores[storeIndex.value] = elementModel
            }

            is UTestGetFieldExpression -> {
                val instanceExpr = uTestInst.instance
                instanceExpr?.let { processInst(it) }

                val instanceModel = exprToModelCache[instanceExpr]
                    ?: error("UtModel for $instanceExpr should have also been created")

                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestInst.type.classId,
                    modelName = "",
                    instantiationCall = UtExecutableCallModel(
                        instance = null,
                        executable = utilMethodProvider.getFieldValueMethodId,
                        params = listOf(
                            instanceModel,
                            UtPrimitiveModel(uTestInst.field.type.classId.name),
                            UtPrimitiveModel(uTestInst.field.name),
                            ),
                    ),
                )

                exprToModelCache[uTestInst] = newModel
            }

            is UTestGetStaticFieldExpression -> {
                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestInst.type.classId,
                    modelName = "",
                    instantiationCall = UtExecutableCallModel(
                        instance = null,
                        executable = utilMethodProvider.getStaticFieldValueMethodId,
                        params = listOf(
                            UtPrimitiveModel(uTestInst.field.type.classId.name),
                            UtPrimitiveModel(uTestInst.field.name),
                        ),
                    ),
                )

                exprToModelCache[uTestInst] = newModel
            }

            // TODO: Is in correct to process [UTestMockObject] and [UTestGlobalMock] similarly?
            is UTestMockObject -> {
                val fields = mutableMapOf<FieldId, UtModel>()
                val mocks = mutableMapOf<ExecutableId, List<UtModel>>()

                val newModel = UtCompositeModel(
                    id = idGenerator.createId(),
                    classId = uTestInst.type.classId,
                    isMock = true,
                    fields = fields,
                    mocks = mocks,
                )
                exprToModelCache[uTestInst] = newModel

                fields += uTestInst.fields
                    .entries
                    .associate { (jcField, uTestExpr) ->
                        val fieldId = FieldId(jcField.type.classId, jcField.name)

                        processInst(uTestExpr)
                        val fieldModel = exprToModelCache[uTestExpr]
                            ?: error("UtModel for $uTestExpr should have also been created")
                        fieldId to fieldModel
                    }

                mocks += uTestInst.methods
                    .entries
                    .associate { (jcMethod, uTestExprs) ->
                        val executableId: ExecutableId = jcMethod.toExecutableId()
                        val models = uTestExprs.map { expr ->
                            processInst(expr)
                            exprToModelCache[expr] ?: error("UtModel for $expr should have also been created")
                        }

                        executableId to models
                    }
            }

            is UTestSetFieldStatement -> {
                val instanceExpr = uTestInst.instance

                instanceExpr?.let { processInst(it) }
                val instanceModel = exprToModelCache[instanceExpr]
                    ?: error("UtModel for $instanceExpr should have also been created")
                require(instanceModel is UtAssembleModel)

                val fieldType = uTestInst.field.type.classId
                val fieldName = uTestInst.field.name

                val setValueExpr = uTestInst.value
                processInst(setValueExpr)
                val setValueModel = exprToModelCache[setValueExpr]
                    ?: error("UtModel for $setValueExpr should have also been created")

                val methodCall = UtExecutableCallModel(
                    instance = instanceModel,
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

            is UTestSetStaticFieldStatement -> {
                // TODO: seems we do not need this snippet as we store only expressions in cache
                val fieldType = uTestInst.field.type.classId
                val fieldName = uTestInst.field.name

                val setValueExpr = uTestInst.value
                processInst(setValueExpr)
                val setValueModel = exprToModelCache[setValueExpr]
                    ?: error("UtModel for $setValueExpr should have also been created")

                val methodCall = UtExecutableCallModel(
                    instance = null,
                    executable = utilMethodProvider.setStaticFieldMethodId,
                    params = listOf(
                        UtPrimitiveModel(fieldType.name),
                        UtPrimitiveModel(fieldName),
                        setValueModel,
                    ),
                )
            }

            is UTestGlobalMock -> {
                // TODO: collect instrumentations here
            }


            is UTestArithmeticExpression -> error("This expression type is not supported")
            is UTestBinaryConditionExpression -> error("This expression type is not supported")
            is UTestBinaryConditionStatement -> error("This expression type is not supported")
            is UTestStaticMethodCall -> error("This expression type is not supported")

            is UTestCastExpression -> error("This expression type is not supported")

            is UTestArrayGetExpression -> error("This expression type is not supported")
            is UTestArrayLengthExpression -> error("This expression type is not supported")
        }
    }
}