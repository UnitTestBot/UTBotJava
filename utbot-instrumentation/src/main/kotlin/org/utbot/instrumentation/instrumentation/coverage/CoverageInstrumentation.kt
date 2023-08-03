package org.utbot.instrumentation.instrumentation.coverage

import kotlinx.coroutines.runBlocking
import org.utbot.common.withAccessibility
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.InvokeWithStaticsInstrumentation
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.util.CastProbesArrayException
import org.utbot.instrumentation.util.NoProbesArrayException
import java.security.ProtectionDomain
import org.utbot.framework.plugin.api.FieldId
import org.utbot.instrumentation.process.generated.CollectCoverageParams

data class CoverageInfo(
    val methodToInstrRange: Map<String, IntRange>,
    val visitedInstrs: List<Int>
)

/**
 * This instrumentation allows collecting coverage after several calls.
 */
open class CoverageInstrumentation : Instrumentation<Result<*>> {
    private val invokeWithStatics = InvokeWithStaticsInstrumentation()

    /**
     * Invokes a method with the given [methodSignature], the declaring class of which is [clazz], with the supplied
     * [arguments] and [parameters]. Supports static environment.
     *
     * @return `Result.success` with wrapped result in case of successful call and
     * `Result.failure` with wrapped target exception otherwise.
     */
    override fun invoke(
        clazz: Class<*>,
        methodSignature: String,
        arguments: ArgumentList,
        parameters: Any?
    ): Result<*> {
        val probesFieldName: String = Settings.PROBES_ARRAY_NAME
        val visitedLinesField = clazz.fields.firstOrNull { it.name == probesFieldName }
            ?: throw NoProbesArrayException(clazz, Settings.PROBES_ARRAY_NAME)

        return visitedLinesField.withAccessibility {
            invokeWithStatics.invoke(clazz, methodSignature, arguments, parameters)
        }
    }

    override fun getStaticField(fieldId: FieldId): Result<*> =
        invokeWithStatics.getStaticField(fieldId)

    /**
     * Collects coverage from the given [clazz] via reflection.
     */
    open fun <T : Any> collectCoverageInfo(clazz: Class<out T>): CoverageInfo {
        val probesFieldName: String = Settings.PROBES_ARRAY_NAME
        val visitedLinesField = clazz.fields.firstOrNull { it.name == probesFieldName }
            ?: throw NoProbesArrayException(clazz, Settings.PROBES_ARRAY_NAME)

        return visitedLinesField.withAccessibility {
            val visitedLines = visitedLinesField.get(null) as? BooleanArray
                ?: throw CastProbesArrayException()

            val methodToInstrRange = methodToCollectCoverage(clazz)

            val res = CoverageInfo(methodToInstrRange, visitedLines.mapIndexed { idx, b ->
                if (b) idx else null
            }.filterNotNull())

            visitedLines.fill(false, 0, visitedLines.size)

            res
        }
    }

    protected open fun <T : Any> methodToCollectCoverage(clazz: Class<out T>): Map<String, IntRange> {
        return Instrumenter(clazz).computeMapOfRanges()
    }

    /**
     * Transforms bytecode such way that it becomes possible to get a coverage.
     *
     * Adds set of instructions which marks the executed instruction as completed. Uses static boolean array for this.
     */
    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        val instrumenter = Instrumenter(classfileBuffer)

        val staticArrayStrategy = StaticArrayStrategy(className, Settings.PROBES_ARRAY_NAME)
        instrumenter.visitInstructions(staticArrayStrategy)
        instrumenter.addStaticField(staticArrayStrategy)

        return instrumenter.classByteCode
    }

    object Factory : Instrumentation.Factory<Result<*>, CoverageInstrumentation> {
        override fun create(): CoverageInstrumentation = CoverageInstrumentation()
    }
}

/**
 * Extension function for the [ConcreteExecutor], which allows to collect the coverage of the given [clazz].
 */
fun ConcreteExecutor<Result<*>, CoverageInstrumentation>.collectCoverage(clazz: Class<*>): CoverageInfo = runBlocking {
    withProcess {
        val clazzByteArray = kryoHelper.writeObject(clazz)

        kryoHelper.readObject(instrumentedProcessModel.collectCoverage.startSuspending(lifetime, CollectCoverageParams(clazzByteArray)).coverageInfo)
    }
}