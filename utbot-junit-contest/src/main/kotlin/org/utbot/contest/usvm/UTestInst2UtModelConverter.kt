package org.utbot.contest.usvm

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
import org.usvm.instrumentation.testcase.api.UTestFloatExpression
import org.usvm.instrumentation.testcase.api.UTestGetFieldExpression
import org.usvm.instrumentation.testcase.api.UTestGetStaticFieldExpression
import org.usvm.instrumentation.testcase.api.UTestInst
import org.usvm.instrumentation.testcase.api.UTestIntExpression
import org.usvm.instrumentation.testcase.api.UTestLongExpression
import org.usvm.instrumentation.testcase.api.UTestMethodCall
import org.usvm.instrumentation.testcase.api.UTestMock
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
import org.utbot.framework.plugin.api.UtModel
import org.utbot.framework.plugin.api.UtNullModel
import org.utbot.framework.plugin.api.UtPrimitiveModel
import org.utbot.framework.plugin.api.util.id
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.fuzzer.ReferencePreservingIntIdGenerator

class UTestInst2UtModelConverter(
    private val utilMethodProvider: UtilMethodProvider
) {

    private val idGenerator = ReferencePreservingIntIdGenerator()
    private val exprToModelCache = mutableMapOf<UTestInst, UtModel>()

    fun convert(uTestInst: UTestInst): UtModel {
        exprToModelCache[uTestInst]?.let { return it }

        return when (uTestInst) {
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
                newModel
            }

            is UTestConstructorCall -> {
                val constructorCall = UtExecutableCallModel(
                    instance = null,
                    executable = uTestInst.method.toExecutableId(),
                    params = uTestInst.args.map { convert(it) },
                )

                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestInst.type.classId,
                    modelName = "",
                    instantiationCall = constructorCall,
                )

                exprToModelCache[uTestInst] = newModel
                newModel
            }

            is UTestMethodCall -> {
                val instanceModel = uTestInst.instance?.let { convert(it) }
                require(instanceModel is UtAssembleModel)

                val methodCall = UtExecutableCallModel(
                    instance = instanceModel,
                    executable = uTestInst.method.toExecutableId(),
                    params = uTestInst.args.map { convert(it) },
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
                newModel
            }

            is UTestClassExpression -> UtClassRefModel(
                    id = idGenerator.createId(),
                    classId = uTestInst.javaClass.id,
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
                newModel
            }

            is UTestArraySetStatement -> {
                val arrayModel = exprToModelCache[uTestInst.arrayInstance]
                requireNotNull(arrayModel)
                require(arrayModel is UtArrayModel)

                require(uTestInst.index is UTestIntExpression)
                val storeIndex = uTestInst.index as UTestIntExpression

                arrayModel.stores[storeIndex.value] = convert(uTestInst.setValueExpression)

                arrayModel
            }

            is UTestGetFieldExpression -> {
                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = uTestInst.type.classId,
                    modelName = "",
                    instantiationCall = UtExecutableCallModel(
                        instance = null,
                        executable = utilMethodProvider.getFieldValueMethodId,
                        params = listOf(
                            convert(uTestInst.instance),
                            UtPrimitiveModel(uTestInst.field.type.classId.name),
                            UtPrimitiveModel(uTestInst.field.name),
                            ),
                    ),
                )

                exprToModelCache[uTestInst] = newModel
                newModel
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
                newModel
            }

            // TODO: Is in correct to process [UTestMockObject] and [UTestGlobalMock] similarly?
            is UTestMock -> {
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
                        val fieldModel = convert(uTestExpr)
                        fieldId to fieldModel
                    }

                mocks += uTestInst.methods
                    .entries
                    .associate { (jcMethod, uTestExprs) ->
                        val executableId: ExecutableId = jcMethod.toExecutableId()
                        val models = uTestExprs.map { expr -> convert(expr) }

                        executableId to models
                    }

                newModel
            }

            is UTestSetFieldStatement -> {
                val instanceModel = uTestInst.instance?.let { convert(it) }
                require(instanceModel is UtAssembleModel)
                val fieldType = uTestInst.field.type.classId
                val fieldName = uTestInst.field.name

                val methodCall = UtExecutableCallModel(
                    instance = instanceModel,
                    executable = utilMethodProvider.setFieldMethodId,
                    params = listOf(
                        convert(uTestInst.instance),
                        UtPrimitiveModel(fieldType.name),
                        UtPrimitiveModel(fieldName),
                        convert(uTestInst.value),
                    ),
                )

                instanceModel?.let {
                    (it.modificationsChain as MutableList).add(methodCall)
                }

                instanceModel
            }

            is UTestSetStaticFieldStatement -> {
                val fieldType = uTestInst.field.type.classId
                val fieldName = uTestInst.field.name

                val methodCall = UtExecutableCallModel(
                    instance = null,
                    executable = utilMethodProvider.setStaticFieldMethodId,
                    params = listOf(
                        UtPrimitiveModel(fieldType.name),
                        UtPrimitiveModel(fieldName),
                        convert(uTestInst.value),
                    ),
                )

                val newModel = UtAssembleModel(
                    id = idGenerator.createId(),
                    classId = objectClassId,
                    modelName = "",
                    instantiationCall = methodCall,
                )

                exprToModelCache[uTestInst] = newModel
                newModel
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