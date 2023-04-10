package org.utbot.framework.process

import org.utbot.common.osSpecificJavaExecutable
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.rd.rdPortArgument
import java.io.File
import kotlin.io.path.pathString

abstract class AbstractRDProcessCompanion(
    private val debugPort: Int,
    private val runWithDebug: Boolean,
    private val suspendExecutionInDebugMode: Boolean
) {
    private val javaExecutablePathString get() =
        JdkInfoService.provide().path.resolve("bin${File.separatorChar}${osSpecificJavaExecutable()}")

    protected abstract fun obtainProcessSpecificCommandLineArgs(): List<String>

    protected fun obtainProcessCommandLine(port: Int): List<String> = buildList {
        addAll(obtainCommonProcessCommandLineArgs())
        addAll(obtainProcessSpecificCommandLineArgs())
        add(rdPortArgument(port))
    }

    private fun obtainCommonProcessCommandLineArgs(): List<String> = buildList {
        val suspendValue = if (suspendExecutionInDebugMode) "y" else "n"
        val debugArgument =
            "-agentlib:jdwp=transport=dt_socket,server=n,suspend=${suspendValue},quiet=y,address=$debugPort"
                .takeIf { runWithDebug }

        add(javaExecutablePathString.pathString)
        val javaVersionSpecificArgs = OpenModulesContainer.javaVersionSpecificArguments
        if (javaVersionSpecificArgs.isNotEmpty()) {
            addAll(javaVersionSpecificArgs)
        }
        debugArgument?.let { add(it) }
    }
}