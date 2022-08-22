package org.utbot.framework.plugin.services

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Singleton to enable abstract access to the working directory.
 *
 * Used in [org.utbot.instrumentation.process.ChildProcessRunner].
 * The purpose is to use the same working directory in [org.utbot.instrumentation.ConcreteExecutor]
 * and in the test runs.
 */
object WorkingDirService : PluginService<Path> {
    var workingDirProvider: WorkingDirProvider = WorkingDirDefaultProvider()

    override fun provide(): Path = workingDirProvider.workingDir
}

abstract class WorkingDirProvider {
    abstract val workingDir: Path
}

open class WorkingDirDefaultProvider : WorkingDirProvider() {
    override val workingDir: Path
        get() = Paths.get(System.getProperty("user.dir"))
}