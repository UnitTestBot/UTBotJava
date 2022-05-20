package org.utbot.instrumentation.agent

import org.utbot.common.asPathToFile
import org.utbot.framework.plugin.api.util.UtContext
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

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
            val pathToClassfile = protectionDomain.codeSource?.location?.path?.asPathToFile()
            return if (pathToClassfile in pathsToUserClasses ||
                packsToAlwaysTransform.any(className::startsWith)
            ) {
                System.err.println("Transforming: $className")
                transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer)
            } else {
                null
            }
        } catch (e: Throwable) {
            System.err.println("Error while transforming: ${e.stackTraceToString()}")
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