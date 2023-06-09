package org.utbot.go.gocodeinstrumentation

import org.utbot.common.FileUtil
import org.utbot.common.scanForResourcesContaining
import org.utbot.go.util.executeCommandByNewProcessOrFail
import org.utbot.go.util.modifyEnvironment
import org.utbot.go.util.parseFromJsonOrFail
import org.utbot.go.util.writeJsonToFileOrFail
import java.io.File
import java.nio.file.Path

object GoPackageInstrumentation {

    fun instrumentGoPackage(
        testedFunctions: List<String>,
        absoluteDirectoryPath: String,
        goExecutableAbsolutePath: Path,
        gopathAbsolutePath: Path
    ): InstrumentationResult {
        val instrumentationTarget = InstrumentationTarget(absoluteDirectoryPath, testedFunctions)
        val instrumentationTargetFileName = createInstrumentationTargetFileName()
        val instrumentationResultFileName = createInstrumentationResultFileName()

        val goPackageInstrumentationSourceDir = extractGoPackageInstrumentationDirectory()
        val instrumentationTargetFile = goPackageInstrumentationSourceDir.resolve(instrumentationTargetFileName)
        val instrumentationResultFile = goPackageInstrumentationSourceDir.resolve(instrumentationResultFileName)

        val goPackageInstrumentationRunCommand = listOf(
            goExecutableAbsolutePath.toString(), "run"
        ) + getGoPackageInstrumentationFilesNames() + listOf(
            "-target",
            instrumentationTargetFile.absolutePath,
            "-result",
            instrumentationResultFile.absolutePath,
        )

        try {
            writeJsonToFileOrFail(instrumentationTarget, instrumentationTargetFile)
            val environment = modifyEnvironment(goExecutableAbsolutePath, gopathAbsolutePath)
            executeCommandByNewProcessOrFail(
                goPackageInstrumentationRunCommand,
                goPackageInstrumentationSourceDir,
                "GoPackageInstrumentation for $instrumentationTarget",
                environment
            )
            return parseFromJsonOrFail<InstrumentationResult>(instrumentationResultFile)
        } finally {
            instrumentationTargetFile.delete()
            instrumentationResultFile.delete()
            goPackageInstrumentationSourceDir.deleteRecursively()
        }
    }

    private fun extractGoPackageInstrumentationDirectory(): File {
        val sourceDirectoryName = "go_package_instrumentation"
        val classLoader = GoPackageInstrumentation::class.java.classLoader

        val containingResourceFile = classLoader.scanForResourcesContaining(sourceDirectoryName).firstOrNull() ?: error(
            "Can't find resource containing $sourceDirectoryName directory."
        )
        if (containingResourceFile.extension != "jar") {
            error("Resource for $sourceDirectoryName directory is expected to be JAR: others are not supported yet.")
        }

        val archiveFilePath = containingResourceFile.toPath()
        return FileUtil.extractDirectoryFromArchive(archiveFilePath, sourceDirectoryName)?.toFile()
            ?: error("Can't find $sourceDirectoryName directory at the top level of JAR ${archiveFilePath.toAbsolutePath()}.")
    }

    private fun getGoPackageInstrumentationFilesNames(): List<String> {
        return listOf(
            "main.go",
            "instrumentator.go",
            "instrumentation_target.go",
            "instrumentation_result.go",
        )
    }

    private fun createInstrumentationTargetFileName(): String {
        return "ut_go_instrumentation_target.json"
    }

    private fun createInstrumentationResultFileName(): String {
        return "ut_go_instrumentation_result.json"
    }
}