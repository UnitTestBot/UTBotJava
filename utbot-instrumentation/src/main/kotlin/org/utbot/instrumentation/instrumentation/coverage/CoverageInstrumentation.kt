package org.utbot.instrumentation.instrumentation.coverage

import org.utbot.common.withAccessibility
import org.utbot.instrumentation.ConcreteExecutor
import org.utbot.instrumentation.Settings
import org.utbot.instrumentation.instrumentation.ArgumentList
import org.utbot.instrumentation.instrumentation.Instrumentation
import org.utbot.instrumentation.instrumentation.InvokeWithStaticsInstrumentation
import org.utbot.instrumentation.instrumentation.instrumenter.Instrumenter
import org.utbot.instrumentation.util.CastProbesArrayException
import org.utbot.instrumentation.util.ChildProcessError
import org.utbot.instrumentation.util.InstrumentationException
import org.utbot.instrumentation.util.NoProbesArrayException
import org.utbot.instrumentation.util.Protocol
import org.utbot.instrumentation.util.UnexpectedCommand
import java.security.ProtectionDomain

data class CoverageInfo(
    val methodToInstrRange: Map<String, IntRange>,
    val visitedInstrs: List<Int>
)

/**
 * This instrumentation allows collecting coverage after several calls.
 */
object CoverageInstrumentation : Instrumentation<Result<*>> {
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


    /**
     * Collects coverage from the given [clazz] via reflection.
     */
    private fun <T : Any> collectCoverageInfo(clazz: Class<out T>): CoverageInfo {
        val probesFieldName: String = Settings.PROBES_ARRAY_NAME
        val visitedLinesField = clazz.fields.firstOrNull { it.name == probesFieldName }
            ?: throw NoProbesArrayException(clazz, Settings.PROBES_ARRAY_NAME)

        return visitedLinesField.withAccessibility {
            val visitedLines = visitedLinesField.get(null) as? BooleanArray
                ?: throw CastProbesArrayException()

            val methodToInstrRange = Instrumenter(clazz).computeMapOfRanges()

            val res = CoverageInfo(methodToInstrRange, visitedLines.mapIndexed { idx, b ->
                if (b) idx else null
            }.filterNotNull())

            visitedLines.fill(false, 0, visitedLines.size)

            res
        }
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

    /**
     * Collects coverage for the class wrapped in [cmd] if [cmd] is [CollectCoverageCommand].
     *
     * @return [CoverageInfoCommand] with wrapped [CoverageInfo] if [cmd] is [CollectCoverageCommand] and `null` otherwise.
     */
    override fun <T : Protocol.InstrumentationCommand> handle(cmd: T): Protocol.Command? = when (cmd) {
        is CollectCoverageCommand<*> -> try {
            CoverageInfoCommand(collectCoverageInfo(cmd.clazz))
        } catch (e: InstrumentationException) {
            Protocol.ExceptionInChildProcess(e)
        }
        else -> null
    }
}

/**
 * This command is sent to the child process from the [ConcreteExecutor] if user wants to collect coverage for the
 * [clazz].
 */
data class CollectCoverageCommand<T : Any>(val clazz: Class<out T>) : Protocol.InstrumentationCommand()

/**
 * This command is sent back to the [ConcreteExecutor] with the [coverageInfo].
 */
data class CoverageInfoCommand(val coverageInfo: CoverageInfo) : Protocol.InstrumentationCommand()

/**
 * Extension function for the [ConcreteExecutor], which allows to collect the coverage of the given [clazz].
 */
fun ConcreteExecutor<Result<*>, CoverageInstrumentation>.collectCoverage(clazz: Class<*>): CoverageInfo {
    val collectCoverageCommand = CollectCoverageCommand(clazz)
    return this.request(collectCoverageCommand) {
        when (it) {
            is CoverageInfoCommand -> it.coverageInfo
            is Protocol.ExceptionInChildProcess -> throw ChildProcessError(it.exception)
            else -> throw UnexpectedCommand(it)
        }
    }
}