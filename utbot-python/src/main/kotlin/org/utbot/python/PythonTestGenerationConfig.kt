package org.utbot.python

import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.python.coverage.CoverageOutputFormat
import org.utbot.python.coverage.PythonCoverageMode
import org.utbot.python.newtyping.mypy.MypyBuildDirectory
import org.utbot.python.newtyping.mypy.MypyInfoBuild
import org.utbot.python.newtyping.mypy.MypyReportLine
import java.nio.file.Path

data class TestFileInformation(
    val testedFilePath: String,
    val testedFileContent: String,
    val moduleName: String,
)

class PythonTestGenerationConfig(
    val pythonPath: String,
    val testFileInformation: TestFileInformation,
    val sysPathDirectories: Set<String>,
    val testedMethods: List<PythonMethodHeader>,
    val timeout: Long,
    val timeoutForRun: Long,
    val testFramework: TestFramework,
    val testSourceRootPath: Path,
    val withMinimization: Boolean,
    val isCanceled: () -> Boolean,
    val runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour,
    val coverageMeasureMode: PythonCoverageMode = PythonCoverageMode.Instructions,
    val sendCoverageContinuously: Boolean = true,
    val coverageOutputFormat: CoverageOutputFormat = CoverageOutputFormat.Lines,
    val usvmConfig: UsvmConfig = UsvmConfig.defaultConfig,
)

data class MypyConfig(
    val mypyStorage: MypyInfoBuild,
    val mypyReportLine: List<MypyReportLine>,
    val mypyBuildDirectory: MypyBuildDirectory,
)

data class UsvmConfig(
    val javaCmd: String,
    val usvmDirectory: String
) {
    companion object {
        val usvmDirectory =
            "/home/vyacheslav/IdeaProjects/NewUTBot/UTBotJava/utbot-python/src/main/kotlin/org/utbot/python/engine/symbolic/usvm-python"
        val usvmJavaCmd = "${System.getProperty("java.home")}/bin/java"
        val defaultConfig = UsvmConfig(usvmJavaCmd, usvmDirectory)
    }
}
