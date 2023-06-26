package org.utbot.python

import org.parsers.python.PythonParser
import org.utbot.python.code.PythonCode
import org.utbot.python.newtyping.PythonFunctionDefinition
import org.utbot.python.newtyping.general.CompositeType
import org.utbot.python.newtyping.getPythonAttributes
import org.utbot.python.newtyping.mypy.MypyAnnotationStorage
import org.utbot.python.newtyping.mypy.MypyReportLine
import org.utbot.python.newtyping.mypy.readMypyAnnotationStorageAndInitialErrors
import org.utbot.python.newtyping.mypy.setConfigFile
import org.utbot.python.utils.RequirementsUtils
import java.io.File

abstract class PythonTestGenerationProcessorNew {
    abstract val configuration: PythonTestGenerationConfig

    fun checkRequirements(): Boolean {
        val requirements = RequirementsUtils.requirements + configuration.testFramework.mainPackage
        if (!configuration.requirementsInstaller.checkRequirements(configuration.pythonPath, requirements)) {
            configuration.requirementsInstaller.installRequirements(configuration.pythonPath, requirements)
            return configuration.requirementsInstaller.checkRequirements(configuration.pythonPath, requirements)
        }
        return true
    }

    fun sourceCodeAnalyze(mypyConfigFile: File): Pair<MypyAnnotationStorage, List<MypyReportLine>> {
//        val mypyConfigFile = setConfigFile(configuration.sysPathDirectories)
        return readMypyAnnotationStorageAndInitialErrors(
            configuration.pythonPath,
            configuration.testFileInformation.testedFilePath,
            configuration.testFileInformation.moduleName,
            mypyConfigFile
        )
    }

    fun testGenerate(mypyStorage: MypyAnnotationStorage, mypyConfigFile: File): List<PythonTestSet> {
        val startTime = System.currentTimeMillis()

        val testCaseGenerator = PythonTestCaseGenerator(
            withMinimization = configuration.withMinimization,
            directoriesForSysPath = configuration.sysPathDirectories,
            curModule = configuration.testFileInformation.moduleName,
            pythonPath = configuration.pythonPath,
            fileOfMethod = configuration.testFileInformation.testedFilePath,
            isCancelled = configuration.isCanceled,
            timeoutForRun = configuration.timeoutForRun,
            sourceFileContent = configuration.testFileInformation.testedFileContent,
            mypyStorage = mypyStorage,
            mypyReportLine = emptyList(),
            mypyConfigFile = mypyConfigFile,
        )

        val until = startTime + configuration.timeout
        return configuration.testedMethods.mapIndexed { index, methodHeader ->
            val methodsLeft = configuration.testedMethods.size - index
            val localUntil = (until - System.currentTimeMillis()) / methodsLeft + System.currentTimeMillis()
            val method = findMethodByHeader(
                mypyStorage,
                methodHeader,
                configuration.testFileInformation.moduleName,
                configuration.testFileInformation.testedFileContent
            )
            testCaseGenerator.generate(method, localUntil)
        }
    }

    fun testCodeGenerate() {}

    fun saveTests() {}

    private fun findMethodByHeader(
        mypyStorage: MypyAnnotationStorage,
        method: PythonMethodHeader,
        curModule: String,
        sourceFileContent: String
    ): PythonMethod {
        var containingClass: CompositeType? = null
        val containingClassName = method.containingPythonClassId?.simpleName
        val functionDef = if (containingClassName == null) {
            mypyStorage.definitions[curModule]!![method.name]!!.getUtBotDefinition()!!
        } else {
            containingClass =
                mypyStorage.definitions[curModule]!![containingClassName]!!.getUtBotType() as CompositeType
            mypyStorage.definitions[curModule]!![containingClassName]!!.type.asUtBotType.getPythonAttributes().first {
                it.meta.name == method.name
            }
        } as? PythonFunctionDefinition ?: error("Selected method is not a function definition")

        val parsedFile = PythonParser(sourceFileContent).Module()
        val funcDef = PythonCode.findFunctionDefinition(parsedFile, method)

        return PythonMethod(
            name = method.name,
            moduleFilename = method.moduleFilename,
            containingPythonClass = containingClass,
            codeAsString = funcDef.body.source,
            definition = functionDef,
            ast = funcDef.body
        )
    }
}