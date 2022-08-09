package org.utbot.framework.plugin.services

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Singleton to enable abstract access to path to JDK.

 * and in the test runs.
 * This is necessary because the engine can be run from the various starting points, like IDEA plugin, CLI, etc.
 */
data class JdkInfo(
    val path: Path,
    @Suppress("unused")
    val version: String
)

object JdkInfoService : PluginService<JdkInfo> {
    var jdkInfoProvider: JdkInfoProvider = JdkInfoDefaultProvider()

    override fun provide(): JdkInfo = jdkInfoProvider.info
}

interface JdkInfoProvider {
    val info: JdkInfo
}

open class JdkInfoDefaultProvider : JdkInfoProvider {
    override val info: JdkInfo =
        JdkInfo(Paths.get(System.getProperty("java.home")), System.getProperty("java.version"))
}
