package org.utbot.instrumentation.agent

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.utbot.common.asPathToFile
import org.utbot.framework.TraceInstrumentationType
import org.utbot.framework.plugin.api.util.UtContext
import java.lang.instrument.ClassFileTransformer
import java.nio.file.Paths
import java.security.ProtectionDomain
import kotlin.io.path.absolutePathString
import kotlin.properties.Delegates


private val logger = getLogger<DynamicClassTransformer>()

/**
 * Transformer, which will transform only classes with certain names.
 */
class DynamicClassTransformer : ClassFileTransformer {
    lateinit var transformer: ClassFileTransformer

    var useBytecodeTransformation by Delegates.notNull<Boolean>()
    lateinit var traceInstrumentationType: TraceInstrumentationType

    private val pathsToUserClasses = mutableSetOf<String>()

    fun addUserPaths(paths: Iterable<String>) {
        pathsToUserClasses += paths.map { it.asPathToFile() }
    }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray? {
        try {
            // since we got here we have loaded a new class, meaning program is not stuck and some "meaningful"
            // non-repeating actions are performed, so we assume that we should not time out for then next 65 ms
            UtContext.currentContext()?.stopWatch?.stop(compensationMillis = 65)
            val pathToClassfile = protectionDomain.codeSource?.location?.toURI()?.let(Paths::get)?.absolutePathString()
            return if (pathToClassfile in pathsToUserClasses ||
                packsToAlwaysTransform.any(className::startsWith)
            ) {
                transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)?.also {
                    logger.info { "Transformed: $className" }
                }
            } else {
                null
            }
        } catch (e: Throwable) {
            logger.error { "Error while transforming: ${e.stackTraceToString()}" }
            throw e
        } finally {
            UtContext.currentContext()?.stopWatch?.start()
        }
    }

    companion object {
        private val packsToAlwaysTransform = listOf(
            "org/slf4j",
            "org/utbot/instrumentation/warmup"
        )
    }
}