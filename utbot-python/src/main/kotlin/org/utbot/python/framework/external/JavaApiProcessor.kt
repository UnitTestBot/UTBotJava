package org.utbot.python.framework.external

import org.utbot.python.PythonTestGenerationConfig
import org.utbot.python.PythonTestGenerationProcessor
import org.utbot.python.PythonTestSet

class JavaApiProcessor(
    override val configuration: PythonTestGenerationConfig
) : PythonTestGenerationProcessor() {
    override fun saveTests(testsCode: String) {
    }

    override fun notGeneratedTestsAction(testedFunctions: List<String>) {
    }

    override fun processCoverageInfo(testSets: List<PythonTestSet>) {
    }
}