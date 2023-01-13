package org.utbot.instrumentation.agent

import com.jetbrains.rd.util.error
import com.jetbrains.rd.util.getLogger
import com.jetbrains.rd.util.info
import org.utbot.common.asPathToFile
import org.utbot.framework.plugin.api.util.UtContext
import java.lang.instrument.ClassFileTransformer
import java.nio.file.Paths
import java.security.ProtectionDomain
import kotlin.io.path.absolutePathString


private val logger = getLogger("DynamicClassTransformer")

/**
 * Transformer, which will transform only classes with certain names.
 */
class DynamicClassTransformer : ClassFileTransformer {
    lateinit var transformer: ClassFileTransformer

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
            UtContext.currentContext()?.stopWatch?.stop()
            val pathToClassfile = protectionDomain.codeSource?.location?.toURI()?.let(Paths::get)?.absolutePathString()
            return if (pathToClassfile in pathsToUserClasses ||
                packsToAlwaysTransform.any(className::startsWith)
            ) {
                logger.info { "Transforming: $className" }
                transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
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