package org.utbot.python.framework.external

import mu.KLogger
import mu.KotlinLogging
import org.utbot.common.PathUtil.toPath
import org.utbot.framework.UtSettings
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.python.*
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.codegen.model.Pytest
import org.utbot.python.framework.codegen.model.Unittest
import org.utbot.python.utils.RequirementsInstaller
import org.utbot.python.utils.Success
import org.utbot.python.utils.findCurrentPythonModule
import java.io.File

object PythonUtBotJavaApi {
    private val logger: KLogger = KotlinLogging.logger {}

    /**
     * Generate test sets
     *
     * @param testMethods methods for test generation
     * @param pythonPath  a path to the Python executable file
     * @param pythonRunRoot a path to the directory where test sets will be executed
     * @param directoriesForSysPath a collection of strings that specifies the additional search path for modules, usually it is only project root
     * @param timeout a timeout to the test generation process (in milliseconds)
     * @param executionTimeout a timeout to one concrete execution
     */
    @JvmStatic
    fun generateTestSets (
        testMethods: List<PythonTestMethodInfo>,
        pythonPath: String,
        pythonRunRoot: String,
        directoriesForSysPath: Collection<String>,
        timeout: Long,
        executionTimeout: Long = UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis,
    ): List<PythonTestSet> {
        logger.info("Checking requirements...")

        val installer = RequirementsInstaller()
        RequirementsInstaller.checkRequirements(
            installer,
            pythonPath,
            emptyList()
        )
        val processor = initPythonTestGeneratorProcessor(
            testMethods,
            pythonPath,
            pythonRunRoot,
            directoriesForSysPath.toSet(),
            timeout,
            executionTimeout,
        )
        logger.info("Loading information about Python types...")
        val (mypyStorage, _) = processor.sourceCodeAnalyze()
        logger.info("Generating tests...")
        return processor.testGenerate(mypyStorage)
    }

    /**
     * Generate test sets code
     *
     * @param testSets a list of test sets
     * @param pythonRunRoot a path to the directory where test sets will be executed
     * @param directoriesForSysPath a collection of strings that specifies the additional search path for modules, usually it is only project root
     * @param testFramework a test framework (Unittest or Pytest)
     */
    @JvmStatic
    fun renderTestSets (
        testSets: List<PythonTestSet>,
        pythonRunRoot: String,
        directoriesForSysPath: Collection<String>,
        testFramework: TestFramework = Unittest,
    ): String {
        if (testSets.isEmpty()) return ""

        require(testFramework is Unittest || testFramework is Pytest) { "TestFramework should be Unittest or Pytest" }

        testSets.map { it.method.containingPythonClass } .toSet().let {
            require(it.size == 1) { "All test methods should be from one class or only top level" }
            it.first()
        }

        val containingFile = testSets.map { it.method.moduleFilename } .toSet().let {
            require(it.size == 1) { "All test methods should be from one module" }
            it.first()
        }
        val moduleUnderTest = findCurrentPythonModule(directoriesForSysPath, containingFile)
        require(moduleUnderTest is Success)

        val testMethods = testSets.map { it.method.toPythonMethodInfo() }.toSet().toList()

        val processor = initPythonTestGeneratorProcessor(
            testMethods = testMethods,
            pythonRunRoot = pythonRunRoot,
            directoriesForSysPath = directoriesForSysPath.toSet(),
            testFramework = testFramework,
        )
        return processor.testCodeGenerate(testSets)
    }

    /**
     * Generate test sets and render code
     *
     * @param testMethods methods for test generation
     * @param pythonPath  a path to the Python executable file
     * @param pythonRunRoot a path to the directory where test sets will be executed
     * @param directoriesForSysPath a collection of strings that specifies the additional search path for modules, usually it is only project root
     * @param timeout a timeout to the test generation process (in milliseconds)
     * @param executionTimeout a timeout to one concrete execution
     * @param testFramework a test framework (Unittest or Pytest)
     */
    @JvmStatic
    fun generate(
        testMethods: List<PythonTestMethodInfo>,
        pythonPath: String,
        pythonRunRoot: String,
        directoriesForSysPath: Collection<String>,
        timeout: Long,
        executionTimeout: Long = UtSettings.concreteExecutionDefaultTimeoutInInstrumentedProcessMillis,
        testFramework: TestFramework = Unittest,
    ): String {
        val testSets =
            generateTestSets(testMethods, pythonPath, pythonRunRoot, directoriesForSysPath, timeout, executionTimeout)
        return renderTestSets(testSets, pythonRunRoot, directoriesForSysPath, testFramework)
    }

    private fun initPythonTestGeneratorProcessor (
        testMethods: List<PythonTestMethodInfo>,
        pythonPath: String = "",
        pythonRunRoot: String,
        directoriesForSysPath: Set<String>,
        timeout: Long = 60_000,
        timeoutForRun: Long = 2_000,
        testFramework: TestFramework = Unittest,
    ): PythonTestGenerationProcessor {

        val pythonFilePath = testMethods.map { it.moduleFilename }.let {
            require(it.size == 1) {"All test methods should be from one file"}
            it.first()
        }
        val contentFile = File(pythonFilePath)
        val pythonFileContent = contentFile.readText()

        val pythonModule = testMethods.map { it.methodName.moduleName }.let {
            require(it.size == 1) {"All test methods should be from one module"}
            it.first()
        }

        val pythonMethods = testMethods.map {
            PythonMethodHeader(
                it.methodName.name,
                it.moduleFilename,
                it.containingClassName?.let { objName ->
                    PythonClassId(objName.moduleName, objName.name)
                })
        }

        return JavaApiProcessor(
            PythonTestGenerationConfig(
                pythonPath,
                TestFileInformation(pythonFilePath, pythonFileContent, pythonModule),
                directoriesForSysPath,
                pythonMethods,
                timeout,
                timeoutForRun,
                testFramework,
                pythonRunRoot.toPath(),
                true,
                { false },
                RuntimeExceptionTestsBehaviour.FAIL
            )
        )
    }
}
