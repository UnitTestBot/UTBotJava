package org.utbot.python

import org.parsers.python.PythonParser
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.UtClusterInfo
import org.utbot.framework.plugin.api.UtExecutionSuccess
import org.utbot.framework.plugin.api.util.UtContext
import org.utbot.framework.plugin.api.util.withUtContext
import org.utbot.python.code.PythonCode
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonMethodId
import org.utbot.python.framework.api.python.PythonModel
import org.utbot.python.framework.api.python.PythonUtExecution
import org.utbot.python.framework.api.python.RawPythonAnnotation
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.codegen.model.PythonCodeGenerator
import org.utbot.python.framework.codegen.model.PythonImport
import org.utbot.python.framework.codegen.model.PythonSysPathImport
import org.utbot.python.framework.codegen.model.PythonSystemImport
import org.utbot.python.framework.codegen.model.PythonUserImport
import org.utbot.python.newtyping.PythonFunctionDefinition
import org.utbot.python.newtyping.general.CompositeType
import org.utbot.python.newtyping.getPythonAttributes
import org.utbot.python.newtyping.mypy.MypyAnnotationStorage
import org.utbot.python.newtyping.mypy.MypyReportLine
import org.utbot.python.newtyping.mypy.readMypyAnnotationStorageAndInitialErrors
import org.utbot.python.newtyping.pythonName
import org.utbot.python.newtyping.pythonTypeName
import org.utbot.python.utils.RequirementsUtils
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.pathString

// TODO: add asserts that one or less of containing classes and only one file
abstract class PythonTestGenerationProcessorNew {
    abstract val configuration: PythonTestGenerationConfig

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
        val tests = configuration.testedMethods.mapIndexed { index, methodHeader ->
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
        val (notEmptyTests, emptyTestSets) = tests.partition { it.executions.isNotEmpty() }

        if (emptyTestSets.isNotEmpty()) {
            notGeneratedTestsAction(emptyTestSets.map { it.method.name })
        }

        return notEmptyTests
    }

    fun testCodeGenerate(testSets: List<PythonTestSet>): String {
        val containingClassName = getContainingClassName(testSets)
        val classId = PythonClassId(configuration.testFileInformation.moduleName, containingClassName)

        val methodIds = testSets.associate {
            it.method to PythonMethodId(
                classId,
                it.method.name,
                RawPythonAnnotation(pythonAnyClassId.name),
                it.method.arguments.map { argument ->
                    argument.annotation?.let { annotation ->
                        RawPythonAnnotation(annotation)
                    } ?: pythonAnyClassId
                }
            )
        }

        val paramNames = testSets.associate { testSet ->
            var params = testSet.method.arguments.map { it.name }
            if (testSet.method.hasThisArgument) {
                params = params.drop(1)
            }
            methodIds[testSet.method] as ExecutableId to params
        }.toMutableMap()

        val allImports = collectImports(testSets)

        val context = UtContext(this::class.java.classLoader)
        withUtContext(context) {
            val codegen = PythonCodeGenerator(
                classId,
                paramNames = paramNames,
                testFramework = configuration.testFramework,
                testClassPackageName = "",
            )
            val testCode = codegen.pythonGenerateAsStringWithTestReport(
                testSets.map { testSet ->
                    val intRange = testSet.executions.indices
                    val clusterInfo = listOf(Pair(UtClusterInfo("FUZZER"), intRange))
                    CgMethodTestSet(
                        executableId = methodIds[testSet.method] as ExecutableId,
                        executions = testSet.executions,
                        clustersInfo = clusterInfo,
                    )
                },
                allImports
            ).generatedCode
            return testCode
        }
    }

    abstract fun saveTests(testsCode: String)

    abstract fun notGeneratedTestsAction(testedFunctions: List<String>)

    private fun getContainingClassName(testSets: List<PythonTestSet>): String {
        val containingClasses = testSets.map { it.method.containingPythonClass?.pythonName() ?: "TopLevelFunctions" }
        return containingClasses.toSet().first()
    }
    
    private fun collectImports(notEmptyTests: List<PythonTestSet>): Set<PythonImport> {
        val importParamModules = notEmptyTests.flatMap { testSet ->
            testSet.executions.flatMap { execution ->
                val params = (execution.stateAfter.parameters + execution.stateBefore.parameters).toMutableSet()
                val self = mutableListOf(execution.stateBefore.thisInstance, execution.stateAfter.thisInstance)
                if (execution is PythonUtExecution) {
                    params.addAll(execution.stateInit.parameters)
                    self.add(execution.stateInit.thisInstance)
                }
                (params + self)
                    .filterNotNull()
                    .flatMap { utModel ->
                        (utModel as PythonModel).let {
                            it.allContainingClassIds.map { classId ->
                                PythonUserImport(importName_ = classId.moduleName)
                            }
                        }
                    }
            }
        }
        val importResultModules = notEmptyTests.flatMap { testSet ->
            testSet.executions.mapNotNull { execution ->
                if (execution.result is UtExecutionSuccess) {
                    (execution.result as UtExecutionSuccess).let { result ->
                        (result.model as PythonModel).let {
                            it.allContainingClassIds.map { classId ->
                                PythonUserImport(importName_ = classId.moduleName)
                            }
                        }
                    }
                } else null
            }.flatten()
        }
        val rootModule = configuration.testFileInformation.moduleName.split(".").first()
        val testRootModule = PythonUserImport(importName_ = rootModule)
        val sysImport = PythonSystemImport("sys")
        val sysPathImports = relativizePaths(configuration.executionPath, configuration.sysPathDirectories).map { PythonSysPathImport(it) }

        val testFrameworkModule =
            configuration.testFramework.testSuperClass?.let { PythonUserImport(importName_ = (it as PythonClassId).rootModuleName) }

        val allImports = (importParamModules + importResultModules + testRootModule + sysPathImports + listOf(
            testFrameworkModule, sysImport
        )).filterNotNull().toSet()
        return allImports
    }

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

    private fun relativizePaths(rootPath: Path?, paths: Set<String>): Set<String> =
        if (rootPath != null) {
            paths.map { path ->
                rootPath.relativize(Path(path)).pathString
            }.toSet()
        } else {
            paths
        }
}