package org.utbot.framework.concrete

import org.utbot.framework.concrete.constructors.ConstructOnlyUserClassesOrCachedObjectsStrategy
import org.utbot.framework.concrete.constructors.UtModelConstructor
import org.utbot.framework.concrete.mock.InstrumentationContext
import org.utbot.framework.plugin.api.*
import org.utbot.greyboxfuzzer.util.UtFuzzingConcreteExecutionResult
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.InvokeInstrumentation
import org.utbot.instrumentation.instrumentation.et.ExplicitThrowInstruction
import org.utbot.instrumentation.instrumentation.et.TraceHandler
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.instrumentation.mock.MockClassVisitor
import org.utbot.framework.concrete.mock.InstrumentationContext.MockGetter
import java.security.AccessControlException
import java.security.ProtectionDomain
import java.util.*
import kotlin.reflect.jvm.javaMethod

interface UtExecutionInstrumentationWithStatsCollection : Instrumentation<UtFuzzingConcreteExecutionResult> {

    val delegateInstrumentation: InvokeInstrumentation// = InvokeInstrumentation()

    val instrumentationContext: InstrumentationContext// = InstrumentationContext()

    val traceHandler: TraceHandler //= TraceHandler()
    val pathsToUserClasses: MutableSet<String>// = mutableSetOf<String>()
    override fun init(pathsToUserClasses: Set<String>) {
        this.pathsToUserClasses.clear()
        this.pathsToUserClasses += pathsToUserClasses
    }

    override fun getStaticField(fieldId: FieldId): Result<UtModel> =
        delegateInstrumentation.getStaticField(fieldId).map { value ->
            val cache = IdentityHashMap<Any, UtModel>()
            val strategy = ConstructOnlyUserClassesOrCachedObjectsStrategy(
                pathsToUserClasses, cache
            )
            UtModelConstructor(cache, strategy).run {
                construct(value, fieldId.type)
            }
        }


    fun sortOutException(exception: Throwable): UtExecutionFailure {
        if (exception is TimeoutException) {
            return UtTimeoutException(exception)
        }
        if (exception is AccessControlException) {
            return UtSandboxFailure(exception)
        }
        val instrs = traceHandler.computeInstructionList()
        val isNested = if (instrs.isEmpty()) {
            false
        } else {
            instrs.first().callId != instrs.last().callId
        }
        return if (instrs.isNotEmpty() && instrs.last().instructionData is ExplicitThrowInstruction) {
            UtExplicitlyThrownException(IllegalStateException(exception::javaClass.name), isNested)
        } else {
            UtImplicitlyThrownException(IllegalStateException(exception::javaClass.name), isNested)
        }

    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray {
        val instrumenter = Instrumenter(classfileBuffer, loader)

        traceHandler.registerClass(className)
        instrumenter.visitInstructions(traceHandler.computeInstructionVisitor(className))

        val mockClassVisitor = instrumenter.visitClass { writer ->
            MockClassVisitor(
                writer,
                MockGetter::getMock.javaMethod!!,
                MockGetter::checkCallSite.javaMethod!!,
                MockGetter::hasMock.javaMethod!!
            )
        }

        mockClassVisitor.signatureToId.forEach { (method, id) ->
            instrumentationContext.methodSignatureToId += method to id
        }

        return instrumenter.classByteCode
    }

}