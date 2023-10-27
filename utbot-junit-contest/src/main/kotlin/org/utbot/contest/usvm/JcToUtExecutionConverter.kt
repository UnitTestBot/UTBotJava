package org.utbot.contest.usvm

import org.jacodb.api.JcClassOrInterface
import org.jacodb.api.ext.jcdbSignature
import org.usvm.api.JcCoverage
import org.usvm.instrumentation.testcase.descriptor.Descriptor2ValueConverter
import org.usvm.instrumentation.util.enclosingClass
import org.usvm.instrumentation.util.enclosingMethod
import org.utbot.contest.usvm.executor.JcExecution
import org.utbot.framework.plugin.api.Coverage
import org.utbot.framework.plugin.api.Instruction
import org.utbot.framework.plugin.api.MissingState
import org.utbot.framework.plugin.api.UtExecution
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.UtVoidModel
import org.utbot.framework.plugin.api.util.jClass
import org.utbot.framework.plugin.api.util.utContext
import org.utbot.instrumentation.instrumentation.execution.constructors.StateBeforeAwareIdGenerator
import org.utbot.instrumentation.instrumentation.execution.constructors.UtModelConstructor
import org.utbot.instrumentation.instrumentation.execution.constructors.javaStdLibModelWithCompositeOriginConstructors
import java.util.*

class JcToUtExecutionConverter(
    private val instructionIdProvider: InstructionIdProvider
) {
    private val valueConstructor = Descriptor2ValueConverter(utContext.classLoader)

    private val utModelConstructor = UtModelConstructor(
        objectToModelCache = IdentityHashMap(),
        idGenerator = StateBeforeAwareIdGenerator(allPreExistingModels = emptySet()),
        utModelWithCompositeOriginConstructorFinder = { classId ->
            javaStdLibModelWithCompositeOriginConstructors[classId.jClass]?.invoke()
        }
    )

    fun convert(jcExecution: JcExecution): UtExecution? {
        // TODO usvm-sbft: convert everything other than coverage
        return UtUsvmExecution(
            stateBefore = MissingState,
            stateAfter = MissingState,
            result = UtExecutionSuccess(UtVoidModel),
            coverage = convertCoverage(jcExecution.coverage, jcExecution.method.enclosingType.jcClass),
            instrumentation = emptyList()
        )
//        val coverage = Coverage(convertCoverage())
//        return when (jcExecution.uTestExecutionResult) {
//            is UTestExecutionSuccessResult -> {
//
//                TODO("usvm-sbft")
//            }
//            is UTestExecutionExceptionResult -> TODO("usvm-sbft")
//            is UTestExecutionInitFailedResult -> {
//                val exception =
//                    valueConstructor.buildObjectFromDescriptor(jcExecution.uTestExecutionResult.cause) as Throwable
//                logger.error(exception) { "Concrete executor failed" }
//                null
//            }
//            is UTestExecutionFailedResult -> {
//                val exception =
//                    valueConstructor.buildObjectFromDescriptor(jcExecution.uTestExecutionResult.cause) as Throwable
//                if (!jcExecution.uTestExecutionResult.cause.raisedByUserCode)
//                    logger.error(exception) { "Concrete executor failed" }
//                // TODO usvm-sbft
//                null
//            }
//            is UTestExecutionTimedOutResult -> {
//                // TODO usvm-sbft
//                null
//            }
//        }
    }

    private fun convertCoverage(jcCoverage: JcCoverage, jcClass: JcClassOrInterface) = Coverage(
        coveredInstructions = jcCoverage.targetClassToCoverage.values.flatMap { jcClassCoverage ->
            jcClassCoverage.visitedStmts.map {
                val methodSignature = it.enclosingMethod.jcdbSignature
                Instruction(
                    internalName = it.enclosingClass.name.replace('.', '/'),
                    methodSignature = methodSignature,
                    lineNumber = it.lineNumber,
                    id = instructionIdProvider.provideInstructionId(methodSignature, it.location.index)
                )
            }
        },
        // TODO usvm-sbft: maybe add cache here
        // TODO usvm-sbft: make sure static initializers are included into instructions count
        //  I assume they are counted as part of `<clinit>` method
        instructionsCount = jcClass.declaredMethods.sumOf { it.instList.size.toLong() }
    )
}