package org.utbot.framework.plugin.services

import java.nio.file.Path
import java.nio.file.Paths

data class JdkInfo(
    val path: Path,
    val version: Int
)

/**
 * Singleton to enable abstract access to path to JDK.

 * Used in [org.utbot.instrumentation.process.ChildProcessRunner].
 * The purpose is to use the same JDK in [org.utbot.instrumentation.ConcreteExecutor] and in the test runs.
 * This is necessary because the engine can be run from the various starting points, like IDEA plugin, CLI, etc.
 */
object JdkInfoService : PluginService<JdkInfo> {
    var jdkInfoProvider: JdkInfoProvider = JdkInfoDefaultProvider()

    override fun provide(): JdkInfo = jdkInfoProvider.info
}

interface JdkInfoProvider {
    val info: JdkInfo
}

/**
 * Gets [JdkInfo] from the current process.
 */
open class JdkInfoDefaultProvider : JdkInfoProvider {
    override val info: JdkInfo =
        JdkInfo(Paths.get(System.getProperty("java.home")), fetchJavaVersion(System.getProperty("java.version")))
}

fun fetchJavaVersion(javaVersion: String): Int {
    val matcher = "(1\\.)?(\\d+)".toRegex()
    return Integer.parseInt(matcher.find(javaVersion)?.groupValues?.getOrNull(2)!!)
}
