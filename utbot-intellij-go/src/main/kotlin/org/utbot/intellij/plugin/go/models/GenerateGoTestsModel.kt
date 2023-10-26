package org.utbot.intellij.plugin.go.models

import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.goide.psi.GoMethodDeclaration
import com.intellij.openapi.project.Project
import org.utbot.go.logic.GoUtTestsGenerationConfig
import org.utbot.go.logic.TestsGenerationMode
import java.nio.file.Path

/**
 * Contains information about Go tests generation task required for intellij plugin logic.
 *
 * targetFunctions: all possible functions to generate tests for;
 * focusedTargetFunctions: such target functions that user is focused on while plugin execution;
 * selectedFunctions: finally selected functions to generate tests for;
 * goExecutableAbsolutePath: self-explanatory;
 * eachFunctionExecutionTimeoutMillis: timeout in milliseconds for each fuzzed function execution.
 */
data class GenerateGoTestsModel(
    val project: Project,
    val goExecutableAbsolutePath: Path,
    val gopathAbsolutePath: Path,
    val targetFunctions: Set<GoFunctionOrMethodDeclaration>,
    val focusedTargetFunctions: Set<GoFunctionOrMethodDeclaration>,
) {
    lateinit var selectedFunctions: Set<GoFunctionDeclaration>
    lateinit var selectedMethods: Set<GoMethodDeclaration>
    var numberOfFuzzingProcess: Int = GoUtTestsGenerationConfig.DEFAULT_NUMBER_OF_FUZZING_PROCESSES
    var mode: TestsGenerationMode = TestsGenerationMode.DEFAULT
    var eachFunctionExecutionTimeoutMillis: Long = GoUtTestsGenerationConfig.DEFAULT_EACH_EXECUTION_TIMEOUT_MILLIS
    var allFunctionExecutionTimeoutMillis: Long = GoUtTestsGenerationConfig.DEFAULT_ALL_EXECUTION_TIMEOUT_MILLIS
}