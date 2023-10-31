package org.utbot.contest.usvm

import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.JcMethod
import org.jacodb.api.JcTypedMethod
import org.jacodb.api.cfg.JcInst
import org.jacodb.api.ext.jcdbSignature
import org.usvm.instrumentation.testcase.api.UTestExecutionExceptionResult
import org.usvm.instrumentation.testcase.api.UTestExecutionFailedResult
import org.usvm.instrumentation.testcase.api.UTestExecutionInitFailedResult
import org.usvm.instrumentation.testcase.api.UTestExecutionResult
import org.usvm.instrumentation.testcase.api.UTestExecutionState
import org.usvm.instrumentation.testcase.api.UTestExecutionSuccessResult
import org.usvm.instrumentation.testcase.api.UTestExecutionTimedOutResult
import org.usvm.instrumentation.testcase.descriptor.Descriptor2ValueConverter
import org.usvm.instrumentation.util.enclosingClass
import org.usvm.instrumentation.util.enclosingMethod
import org.usvm.instrumentation.util.toJavaField
import org.utbot.contest.usvm.executor.JcExecution
import org.utbot.framework.codegen.domain.builtin.TestClassUtilMethodProvider
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.EnvironmentModels
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.FieldId
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.util.fieldId
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.objectClassId
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.instrumentation.instrumentation.execution.constructors.StateBeforeAwareIdGenerator
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructor
import org.utbot.instrumentation.instrumentation.execution.constructors.javaStdLibModelWithCompositeOriginConstructors
import java.util.*

class JcToUtExecutionConverter(
    private val instructionIdProvider: InstructionIdProvider,
) {
    //TODO: obtain in somehow from [JcExecution] or somewhere else
    private val testClassId: ClassId = objectClassId
    private val utilMethodProvider = TestClassUtilMethodProvider(testClassId)

    private val modelConverter = JcToUtModelConverter(utilMethodProvider)
    private val instConverter = UTestInst2UtModelConverter(utilMethodProvider)

    fun convert(jcExecution: JcExecution): UtExecution? {
        val coverage = convertCoverage(getTrace(jcExecution.uTestExecutionResult), jcExecution.method.enclosingType.jcClass)

        val executionResult = jcExecution.uTestExecutionResult
        return when (executionResult) {
            is UTestExecutionSuccessResult -> {
                val result = UtExecutionSuccess(modelConverter.convert(executionResult.result))


                UtUsvmExecution(
                    stateBefore = MissingState,
                    stateAfter = MissingState,
                    result = result,
                    coverage = coverage,
                    instrumentation = emptyList()
                )
            }

            is UTestExecutionExceptionResult -> TODO("usvm-sbft")
            is UTestExecutionInitFailedResult -> {
                val exception =
                    valueConstructor.buildObjectFromDescriptor(jcExecution.uTestExecutionResult.cause) as Throwable
                logger.error(exception) { "Concrete executor failed" }
                null
            }

            is UTestExecutionFailedResult -> {
                val exception =
                    valueConstructor.buildObjectFromDescriptor(jcExecution.uTestExecutionResult.cause) as Throwable
                if (!jcExecution.uTestExecutionResult.cause.raisedByUserCode)
                    logger.error(exception) { "Concrete executor failed" }
                // TODO usvm-sbft
                null
            }

            is UTestExecutionTimedOutResult -> {
                // TODO usvm-sbft
                null
            }
        }

    }

    private fun getTrace(executionResult: UTestExecutionResult): List<JcInst>? = when (executionResult) {
        is UTestExecutionExceptionResult -> executionResult.trace
        is UTestExecutionInitFailedResult -> executionResult.trace
        is UTestExecutionSuccessResult -> executionResult.trace
        is UTestExecutionFailedResult -> emptyList()
        is UTestExecutionTimedOutResult -> emptyList()
    }

    private fun convertState(state: UTestExecutionState, method: JcTypedMethod): EnvironmentModels {
        val thisInstance = instConverter.convert(state.instanceDescriptor?.origin!!)
        val parameters = state.argsDescriptors.map { instConverter.convert(it?.origin!!) }
        val statics = state.statics
            .entries
            .associate { (jcField, uTestDescr) ->
                val fieldType = jcField.toJavaField(utContext.classLoader)!!.fieldId.type
                val fieldId = FieldId(fieldType, jcField.name)
                val fieldModel = modelConverter.convert(uTestDescr)

                fieldId to fieldModel
            }
        val executableId: ExecutableId = method.method.toExecutableId()
        return EnvironmentModels(thisInstance, parameters, statics, executableId)
    }

    private fun convertCoverage(jcCoverage: List<JcInst>?, jcClass: JcClassOrInterface) = Coverage(
        coveredInstructions = jcCoverage.orEmpty().map {
            val methodSignature = it.enclosingMethod.jcdbSignature
            Instruction(
                internalName = it.enclosingClass.name.replace('.', '/'),
                methodSignature = methodSignature,
                lineNumber = it.lineNumber,
                id = instructionIdProvider.provideInstructionId(methodSignature, it.location.index)
            )
        },
        // TODO usvm-sbft: maybe add cache here
        // TODO usvm-sbft: make sure static initializers are included into instructions count
        //  I assume they are counted as part of `<clinit>` method
        instructionsCount = jcClass.declaredMethods.sumOf { it.instList.size.toLong() }
    )
}