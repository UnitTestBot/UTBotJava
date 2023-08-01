package org.utbot.cli.language.python

import mu.KLogger
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.PythonTestSet

class PythonCliProcessor(
    override val configuration: PythonTestGenerationConfig,
    private val output: String,
    private val logger: KLogger,
    private val coverageOutput: String?,
) : PythonTestGenerationProcessor() {

    override fun saveTests(testsCode: String) {
        writeToFileAndSave(output, testsCode)
    }

    override fun notGeneratedTestsAction(testedFunctions: List<String>) {
        logger.error(
            "Couldn't generate tests for the following functions: ${testedFunctions.joinToString()}"
        )
    }

    override fun processCoverageInfo(testSets: List<PythonTestSet>) {
        val coverageReport = getCoverageInfo(testSets)
        val output = coverageOutput ?: return
        writeToFileAndSave(output, coverageReport)
    }
}