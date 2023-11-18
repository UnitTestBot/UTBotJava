package org.utbot.contest.usvm.converter

import org.jacodb.api.JcClasspath
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
import org.utbot.framework.plugin.api.MethodId
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
import org.utbot.framework.plugin.api.UtStaticMethodInstrumentation
import org.utbot.framework.plugin.api.util.classClassId
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.fuzzer.IdGenerator

class UTestInstToUtModelConverter(
    private val uTest: UTest,
    private val jcClasspath: JcClasspath,
    private val idGenerator: IdGenerator<Int>,
    private val utilMethodProvider: UtilMethodProvider,
) {
    private val exprToModelCache = mutableMapOf<UTestExpression, UtModel>()
    private val instrumentations = mutableListOf<UtInstrumentation>()

    fun processUTest(): UTestAnalysisResult {
        uTest.initStatements.forEach { uInst -> processInst(uInst) }
        removeInstantiationCallFromThisInstanceModificationChain(processExpr(uTest.callMethodExpression))

        return UTestAnalysisResult(instrumentations)
    }

    fun findModelByInst(expr: UTestExpression): UtModel {
        val alreadyCreatedModel = exprToModelCache.getValue(expr)
        removeInstantiationCallFromThisInstanceModificationChain(alreadyCreatedModel)
        return alreadyCreatedModel
    }

    private fun removeInstantiationCallFromThisInstanceModificationChain(model: UtModel) {
        if (model is UtAssembleModel) {
            val instantiationCall = model.instantiationCall
            if (instantiationCall is UtExecutableCallModel) {
                val instanceModel = instantiationCall.instance as? UtAssembleModel
                instanceModel?.let {
                    (it.modificationsChain as MutableList).remove(instantiationCall)
                }
            }
        }
    }

    private fun processInst(uTestInst: UTestInst) {
        when (uTestInst) {
            is UTestExpression -> processExpr(uTestInst)

            is UTestArraySetStatement -> {
                val arrayModel = processExpr(uTestInst.arrayInstance)
                require(arrayModel is UtArrayModel)

                val storeIndex = uTestInst.index as UTestIntExpression
                val elementModel = processExpr(uTestInst.setValueExpression)

                arrayModel.stores[storeIndex.value] = elementModel
            }

            is UTestSetFieldStatement -> {
                val instanceExpr = uTestInst.instance

                val instanceModel = processExpr(instanceExpr)
                require(instanceModel is UtAssembleModel)

                val fieldType = uTestInst.field.enclosingClass.classId
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

                UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.clazz.classId,
                    modelName = "",
                    instantiationCall = createInstanceCall,
                )
            }

            is UTestConstructorCall -> {
                val constructorCall = UtExecutableCallModel(
                    instance = null,
                    executable = uTestExpr.method.toExecutableId(jcClasspath),
                    params = uTestExpr.args.map { arg ->
                        processExpr(arg)
                    },
                )

                UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.type.classId,
                    modelName = "",
                    instantiationCall = constructorCall,
                )
            }

            is UTestMethodCall -> {
                val instanceModel = processExpr(uTestExpr.instance)
                require(instanceModel is UtAssembleModel)

                val methodCall = UtExecutableCallModel(
                    instance = instanceModel,
                    executable = uTestExpr.method.toExecutableId(jcClasspath),
                    params = uTestExpr.args.map { arg -> processExpr(arg) },
                )

                (instanceModel.modificationsChain as MutableList).add(methodCall)

                UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.type.classId,
                    modelName = "",
                    instantiationCall = methodCall,
                )
            }

            is UTestStaticMethodCall -> {
                UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.type.classId,
                    modelName = "",
                    instantiationCall = UtExecutableCallModel(
                        instance = null,
                        executable = uTestExpr.method.toExecutableId(jcClasspath),
                        params = uTestExpr.args.map { arg -> processExpr(arg) },
                    ),
                )
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
                        UtPrimitiveModel(uTestExpr.field.type),
                        UtPrimitiveModel(uTestExpr.field.name),
                    ),
                )

                UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.type.classId,
                    modelName = "",
                    instantiationCall = getFieldCall,
                )
            }

            is UTestGetStaticFieldExpression -> {
                val getStaticFieldCall = UtExecutableCallModel(
                    instance = null,
                    executable = utilMethodProvider.getStaticFieldValueMethodId,
                    params = listOf(
                        UtPrimitiveModel(uTestExpr.field.type),
                        UtPrimitiveModel(uTestExpr.field.name),
                    ),
                )

                UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestExpr.type.classId,
                    modelName = "",
                    instantiationCall = getStaticFieldCall,
                )
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
                        val executableId: ExecutableId = jcMethod.toExecutableId(jcClasspath)
                        val models = uTestExprs.map { expr ->
                            processExpr(expr)
                        }

                        executableId to models
                    }

                newModel
            }

            is UTestGlobalMock -> {
                val methodsToExprs = uTestExpr.methods.entries
                val initMethodExprs = methodsToExprs.filter { it.key.isConstructor }
                val otherMethodsExprs = methodsToExprs.minus(initMethodExprs.toSet())

                otherMethodsExprs
                    .forEach { (jcMethod, uTestExprs) ->
                        val methodId = jcMethod.toExecutableId(jcClasspath) as MethodId
                        val valueModels = uTestExprs.map { expr -> processExpr(expr) }
                        val methodInstrumentation = UtStaticMethodInstrumentation(
                            methodId = methodId,
                            values = valueModels,
                            )

                        instrumentations += methodInstrumentation
                    }

                initMethodExprs
                    .forEach { (jcMethod, uTestExprs) ->
                        val valueModels = uTestExprs.map { expr -> processExpr(expr) }
                        val methodInstrumentation = UtNewInstanceInstrumentation(
                            classId = jcMethod.enclosingClass.classId,
                            instances = valueModels,
                            // [UTestGlobalMock] does not have an equivalent of [callSites],
                            // but it is used only in UtBot instrumentation. We use USVM one, so it is not a problem.
                            callSites = emptySet(),
                            )

                        instrumentations += methodInstrumentation
                    }

                // UtClassRefModel is returned here for consistency with [Descriptor2ValueConverter]
                // which returns Class<*> instance for [UTestGlobalMock] descriptors.
                UtClassRefModel(
                    id = idGenerator.createId(),
                    classId = classClassId,
                    value = uTestExpr.type.classId
                )
            }

            is UTestArithmeticExpression -> error("This expression type is not supported")
            is UTestBinaryConditionExpression -> error("This expression type is not supported")

            is UTestCastExpression -> error("This expression type is not supported")

            is UTestArrayGetExpression -> error("This expression type is not supported")
            is UTestArrayLengthExpression -> error("This expression type is not supported")
        }
    }
}

data class UTestAnalysisResult(
    val instrumentation: List<UtInstrumentation>,
)