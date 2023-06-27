package org.utbot.python

import org.utbot.framework.codegen.domain.TestFramework
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
    val executionPath: Path,
    val withMinimization: Boolean,
    val isCanceled: () -> Boolean,
)