package org.utbot.framework

import java.nio.file.Path

/**
 * Singleton to enable abstract access to path to JDK.
 *
 * Used in [org.utbot.instrumentation.process.ChildProcessRunner].
 * The purpose is to use the same JDK in [org.utbot.instrumentation.ConcreteExecutor]
 * and in the test runs.
 * This is necessary because the engine can be run from the various starting points, like IDEA plugin, CLI, etc.
 */
object JdkPathService {
    var jdkPathProvider: JdkPathProvider = JdkPathDefaultProvider()

    // Kotlin delegates do not support changing in runtime, so use simple getter
    val jdkPath: Path
        get() = jdkPathProvider.jdkPath

    val jdkVersion: String
        get() = jdkPathProvider.jdkVersion
}
