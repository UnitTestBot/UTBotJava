package org.utbot.python.framework.codegen.model

import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.codegen.model.CodeGeneratorResult

import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.codegen.model.constructor.TestClassModel
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.tree.*
import org.utbot.framework.codegen.model.util.CgPrinterImpl
import org.utbot.framework.codegen.model.visitor.CgRendererContext
import org.utbot.framework.plugin.api.*
import org.utbot.python.PythonMethod
import org.utbot.python.code.AnnotationProcessor.getModulesFromAnnotation
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.framework.codegen.model.constructor.tree.PythonCgTestClassConstructor
import org.utbot.python.framework.codegen.model.constructor.visitor.CgPythonRenderer
import org.utbot.python.framework.codegen.model.tree.CgPythonDict
import org.utbot.python.framework.codegen.model.tree.CgPythonFunctionCall
import org.utbot.python.framework.codegen.model.tree.CgPythonList

class PythonCodeGenerator(
    classUnderTest: ClassId,
    paramNames: MutableMap<ExecutableId, List<String>> = mutableMapOf(),
    testFramework: TestFramework = TestFramework.defaultItem,
    mockFramework: MockFramework = MockFramework.defaultItem,
    staticsMocking: StaticsMocking = StaticsMocking.defaultItem,
    forceStaticMocking: ForceStaticMocking = ForceStaticMocking.defaultItem,
    generateWarningsForStaticMocking: Boolean = true,
    parameterizedTestSource: ParametrizedTestSource = ParametrizedTestSource.defaultItem,
    runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.defaultItem,
    hangingTestsTimeout: HangingTestsTimeout = HangingTestsTimeout(),
    enableTestsTimeout: Boolean = true,
    testClassPackageName: String = classUnderTest.packageName
) : CodeGenerator(
    classUnderTest=classUnderTest,
    paramNames=paramNames,
    generateUtilClassFile = true,
    testFramework=testFramework,
    mockFramework=mockFramework,
    staticsMocking=staticsMocking,
    forceStaticMocking=forceStaticMocking,
    generateWarningsForStaticMocking=generateWarningsForStaticMocking,
    parameterizedTestSource=parameterizedTestSource,
    runtimeExceptionTestsBehaviour=runtimeExceptionTestsBehaviour,
    hangingTestsTimeout=hangingTestsTimeout,
    enableTestsTimeout=enableTestsTimeout,
    testClassPackageName=testClassPackageName
) {
    override var context: CgContext = CgContext(
        classUnderTest = classUnderTest,
        paramNames = paramNames,
        testFramework = testFramework,
        mockFramework = mockFramework,
        cgLanguageAssistant = PythonCgLanguageAssistant,
        parametrizedTestSource = parameterizedTestSource,
        staticsMocking = staticsMocking,
        forceStaticMocking = forceStaticMocking,
        generateWarningsForStaticMocking = generateWarningsForStaticMocking,
        runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour,
        hangingTestsTimeout = hangingTestsTimeout,
        enableTestsTimeout = enableTestsTimeout,
        testClassPackageName = testClassPackageName
    )

    fun pythonGenerateAsStringWithTestReport(
        cgTestSets: List<CgMethodTestSet>,
        importModules: Set<PythonImport>,
        testClassCustomName: String? = null,
    ): CodeGeneratorResult = withCustomContext(testClassCustomName) {
        context.withTestClassFileScope {
            val testClassModel = TestClassModel(classUnderTest, cgTestSets)
            context.collectedImports.addAll(importModules)
            val testClassFile = PythonCgTestClassConstructor(context).construct(testClassModel)
            CodeGeneratorResult(renderClassFile(testClassFile), testClassFile.testsGenerationReport)
        }
    }

    fun generateFunctionCall(
        method: PythonMethod,
        methodArguments: List<UtModel>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        additionalModules: Set<String> = emptySet(),
        fileForOutputName: String
    ): String {
        val cgRendererContext = CgRendererContext.fromCgContext(context)
        val printer = CgPrinterImpl()
        val renderer = CgPythonRenderer(cgRendererContext, printer)

        val executorFunctionName = "run_calculate_function_value"
        val executorModuleName = "utbot_executor.executor"

        val importExecutor = PythonUserImport(executorFunctionName, executorModuleName)
        val importSys = PythonSystemImport("sys")
        val importSysPaths = directoriesForSysPath.map { PythonSysPathImport(it) }
        val importFunction = PythonUserImport("*", moduleToImport)
        val imports =
            listOf(importSys) + importSysPaths + listOf(importExecutor, importFunction) + additionalModules.map { PythonUserImport(it) }

        val containingClass = method.containingPythonClassId
        val functionName =
            if (containingClass == null)
                CgLiteral(pythonAnyClassId, method.name)
            else
                CgLiteral(pythonAnyClassId, "${containingClass.name}.${method.name}")

        val arguments = method.arguments.map { argument ->
            CgVariable(argument.name, argument.annotation?.let { PythonClassId(it) } ?: pythonAnyClassId)
        }

        val parameters = methodArguments.zip(arguments).map { (model, argument) ->
            CgAssignment(
                argument,
                CgLiteral(model.classId, model.toString())
            )
        }

        val args = CgPythonList(emptyList())
        val kwargs = CgPythonDict(
            arguments.associateBy { argument -> CgLiteral(pythonStrClassId, "'${argument.name}'") }
        )

        val fullpath = CgLiteral(pythonStrClassId, "'${method.moduleFilename}'")

        val outputPath = CgLiteral(pythonStrClassId, "'$fileForOutputName'")

        val executorCall = CgPythonFunctionCall(
            pythonNoneClassId,
            executorFunctionName,
            listOf(
                functionName,
                args,
                kwargs,
                fullpath,
                outputPath,
            )
        )

        imports.forEach {
            renderer.renderPythonImport(it)
        }
        parameters.forEach { it.accept(renderer) }
        executorCall.accept(renderer)
        return renderer.toString()
    }

    fun generateMypyCheckCode(
        method: PythonMethod,
        methodAnnotations: Map<String, NormalizedPythonAnnotation>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String
    ): String {
        val cgRendererContext = CgRendererContext.fromCgContext(context)
        val printer = CgPrinterImpl()
        val renderer = CgPythonRenderer(cgRendererContext, printer)

        val importSys = PythonSystemImport("sys")
        val importTyping = PythonSystemImport("typing")
        val importSysPaths = directoriesForSysPath.map { PythonSysPathImport(it) }
        val importFunction = PythonUserImport("*", moduleToImport)
        val additionalModules = methodAnnotations.values.flatMap { annotation ->
                getModulesFromAnnotation(annotation).map { PythonUserImport(it) }
        }
        val imports = listOf(importSys, importTyping) + importSysPaths + (listOf(importFunction) + additionalModules).toSet().toList()

        imports.forEach { renderer.renderPythonImport(it) }

        val parameters = method.arguments.map { argument ->
            "${argument.name}: ${methodAnnotations[argument.name] ?: pythonAnyClassId.name}"
        }

        val functionPrefix = "__mypy_check"
        val functionName = "def ${functionPrefix}_{method.name}(${parameters.joinToString(", ")}):"  // TODO: in future can be "async def"

        val mypyCheckCode = listOf(
            renderer.toString(),
            "",
            functionName,
        ) + method.codeLines().map { "    $it" }
        return mypyCheckCode.joinToString("\n")
    }
}