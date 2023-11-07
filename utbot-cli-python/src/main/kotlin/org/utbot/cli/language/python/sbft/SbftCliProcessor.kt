package org.utbot.cli.language.python.sbft

import mu.KLogger
import org.utbot.cli.language.python.writeToFileAndSave
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.PythonTestSet
import java.io.File

class SbftCliProcessor(
    override val configuration: PythonTestGenerationConfig,
) : PythonTestGenerationProcessor() {
    override fun saveTests(testsCode: String) { }

    override fun notGeneratedTestsAction(testedFunctions: List<String>) {}

    override fun processCoverageInfo(testSets: List<PythonTestSet>) {}
}
