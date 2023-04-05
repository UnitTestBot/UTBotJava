package org.utbot.framework.process

import org.utbot.common.osSpecificJavaExecutable
import org.utbot.framework.plugin.services.JdkInfoService
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

object CommonProcessArgs {
    private val javaExecutablePathString: Path
        get() = JdkInfoService.provide().path.resolve("bin${File.separatorChar}${osSpecificJavaExecutable()}")

    fun obtainCommonProcessCommandLineArgs(
        debugPort: Int,
        runWithDebug: Boolean,
        suspendExecutionInDebugMode: Boolean,
    ): List<String> = buildList {
        val suspendValue = if (suspendExecutionInDebugMode) "y" else "n"
        val debugArgument = "-agentlib:jdwp=transport=dt_socket,server=n,suspend=${suspendValue},quiet=y,address=$debugPort"
            .takeIf { runWithDebug }

        add(javaExecutablePathString.pathString)
        val javaVersionSpecificArgs = OpenModulesContainer.javaVersionSpecificArguments
        if (javaVersionSpecificArgs.isNotEmpty()) {
            addAll(javaVersionSpecificArgs)
        }
        debugArgument?.let { add(it) }
    }
}
