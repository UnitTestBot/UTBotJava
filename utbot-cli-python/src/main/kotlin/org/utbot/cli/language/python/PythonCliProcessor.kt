package org.utbot.cli.language.python

import mu.KotlinLogging
import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.PythonTestSet

private val logger = KotlinLogging.logger {}

class PythonCliProcessor(
    override val configuration: PythonTestGenerationConfig,
    private val testWriter: TestWriter,
    private val coverageOutput: String?,
    private val executionCounterOutput: String?,
) : PythonTestGenerationProcessor() {

    override fun saveTests(testsCode: String) {
        testWriter.addTestCode(testsCode)
//        writeToFileAndSave(output, testsCode)
    }

    override fun notGeneratedTestsAction(testedFunctions: List<String>) {
        logger.error(
            "Couldn't generate tests for the following functions: ${testedFunctions.joinToString()}"
        )
    }

    private fun getExecutionsNumber(testSets: List<PythonTestSet>): Int {
        return testSets.sumOf { it.executionsNumber }
    }

    override fun processCoverageInfo(testSets: List<PythonTestSet>) {
        coverageOutput?.let { output ->
            val coverageReport = getStringCoverageInfo(testSets)
            writeToFileAndSave(output, coverageReport)
        }
        executionCounterOutput?.let { executionOutput ->
            val executionReport = "{\"executions\": ${getExecutionsNumber(testSets)}}"
            writeToFileAndSave(executionOutput, executionReport)
        }
    }
}