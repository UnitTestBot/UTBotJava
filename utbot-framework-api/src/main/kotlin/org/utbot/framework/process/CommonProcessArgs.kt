package org.utbot.framework.process

import org.utbot.common.osSpecificJavaExecutable
import org.utbot.framework.plugin.services.JdkInfoService
import org.utbot.rd.rdPortArgument
import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

private val javaExecutablePathString: Path
    get() = JdkInfoService.jdkInfoProvider.info.path.resolve("bin${File.separatorChar}${osSpecificJavaExecutable()}")

// TODO use it in EngineProcess and InstrumentedProcess
fun withCommonProcessCommandLineArgs(
    processSpecificArgs: List<String>,
    debugPort: Int,
    runWithDebug: Boolean,
    suspendExecutionInDebugMode: Boolean,
    rdPort: Int
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
    addAll(processSpecificArgs)
    add(rdPortArgument(rdPort))
}