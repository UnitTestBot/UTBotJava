package org.utbot.python.framework.codegen.model

import org.utbot.framework.codegen.CodeGenerator
import org.utbot.framework.codegen.CodeGeneratorResult
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.domain.models.CgAssignment
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.codegen.renderer.CgPrinterImpl
import org.utbot.framework.codegen.renderer.CgRendererContext
import org.utbot.framework.codegen.tree.CgTestClassConstructor.CgComponents.clearContextRelatedStorage
import org.utbot.python.PythonMethod
import org.utbot.python.code.AnnotationProcessor.getModulesFromAnnotation
import org.utbot.python.framework.api.python.NormalizedPythonAnnotation
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.util.pythonAnyClassId
import org.utbot.python.framework.api.python.util.pythonNoneClassId
import org.utbot.python.framework.api.python.util.pythonStrClassId
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.UtModel
import org.utbot.python.framework.api.python.PythonTreeModel
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.framework.codegen.model.constructor.tree.PythonCgTestClassConstructor
import org.utbot.python.framework.codegen.model.constructor.tree.PythonCgVariableConstructor
import org.utbot.python.framework.codegen.model.constructor.visitor.CgPythonRenderer
import org.utbot.python.framework.codegen.model.tree.CgPythonDict
import org.utbot.python.framework.codegen.model.tree.CgPythonFunctionCall
import org.utbot.python.framework.codegen.model.tree.CgPythonList
import org.utbot.python.framework.codegen.model.tree.CgPythonTree

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
    testClassPackageName=testClassPackageName,
    cgLanguageAssistant = PythonCgLanguageAssistant,
) {
    fun pythonGenerateAsStringWithTestReport(
        cgTestSets: List<CgMethodTestSet>,
        importModules: Set<PythonImport>,
        testClassCustomName: String? = null,
    ): CodeGeneratorResult = withCustomContext(testClassCustomName) {
        context.withTestClassFileScope {
            val testClassModel = TestClassModel(classUnderTest, cgTestSets)
            context.collectedImports.addAll(importModules)

            val astConstructor = PythonCgTestClassConstructor(context)
            val renderer = CgAbstractRenderer.makeRenderer(context)

            val testClassFile = astConstructor.construct(testClassModel)
            testClassFile.accept(renderer)

            CodeGeneratorResult(renderer.toString(), astConstructor.testsGenerationReport)
        }
    }

    fun generateFunctionCall(
        method: PythonMethod,
        methodArguments: List<UtModel>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        additionalModules: Set<String> = emptySet(),
        fileForOutputName: String,
        coverageDatabasePath: String,
    ): String = withCustomContext(testClassCustomName = null) {
        context.withTestClassFileScope {
            clearContextRelatedStorage()
            (context.cgLanguageAssistant as PythonCgLanguageAssistant).memoryObjects.clear()

            val renderer = CgAbstractRenderer.makeRenderer(context) as CgPythonRenderer

            val executorFunctionName = "run_calculate_function_value"
            val executorModuleName = "utbot_executor.executor"

            val importExecutor = PythonUserImport(executorFunctionName, executorModuleName)
            val importSys = PythonSystemImport("sys")
            val importSysPaths = directoriesForSysPath.map { PythonSysPathImport(it) }
            val importFunction = PythonUserImport("*", moduleToImport)
            val imports =
                listOf(importSys) + importSysPaths + listOf(importExecutor, importFunction) + additionalModules.map { PythonUserImport(it) }
            imports.forEach {
                renderer.renderPythonImport(it)
            }

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
                if (model is PythonTreeModel) {
                    val obj = (context.cgLanguageAssistant.getVariableConstructorBy(context) as PythonCgVariableConstructor).getOrCreateVariable(model)
                    context.currentBlock.forEach {it.accept(renderer)}
//                    (obj as CgPythonTree).children.forEach { it.accept(renderer) }

                    CgAssignment(
                        argument,
                        (obj as CgPythonTree).value
                    )
                } else {
                    CgAssignment(argument, CgLiteral(model.classId, model.toString()))
                }
            }

            val args = CgPythonList(emptyList())
            val kwargs = CgPythonDict(
                arguments.associateBy { argument -> CgLiteral(pythonStrClassId, "'${argument.name}'") }
            )

        val fullpath = CgLiteral(pythonStrClassId, "'${method.moduleFilename.replace("\\", "\\\\")}'")
        val outputPath = CgLiteral(pythonStrClassId, "'$fileForOutputName'")
        val databasePath = CgLiteral(pythonStrClassId, "'$coverageDatabasePath'")

        val executorCall = CgPythonFunctionCall(
            pythonNoneClassId,
            executorFunctionName,
            listOf(
                databasePath,
                functionName,
                args,
                kwargs,
                fullpath,
                outputPath,
            )
        )

            parameters.forEach { it.accept(renderer) }
            executorCall.accept(renderer)

            renderer.toString()
        }
    }

    fun generateMypyCheckCode(
        method: PythonMethod,
        methodAnnotations: Map<String, NormalizedPythonAnnotation>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        namesInModule: Collection<String>
    ): String {
        val cgRendererContext = CgRendererContext.fromCgContext(context)
        val printer = CgPrinterImpl()
        val renderer = CgPythonRenderer(cgRendererContext, printer)

        val importSys = PythonSystemImport("sys")
        val importTyping = PythonSystemImport("typing")
        val importSysPaths = directoriesForSysPath.map { PythonSysPathImport(it) }
        val importsFromModule = namesInModule.map { name ->
            PythonUserImport(name, moduleToImport)
        }
        val additionalModules = methodAnnotations.values.flatMap { annotation ->
            getModulesFromAnnotation(annotation).map { PythonUserImport(it) }
        }
        val imports = listOf(importSys, importTyping) + importSysPaths + (importsFromModule + additionalModules).toSet().toList()

        imports.forEach { renderer.renderPythonImport(it) }

        val parameters = method.arguments.map { argument ->
            "${argument.name}: ${methodAnnotations[argument.name] ?: pythonAnyClassId.name}"
        }

        val functionPrefix = "__mypy_check"
        val functionName = "def ${functionPrefix}_${method.name}(${parameters.joinToString(", ")}):"  // TODO: in future can be "async def"

        val mypyCheckCode = listOf(
            renderer.toString(),
            "",
            functionName,
        ) + method.codeAsString.split("\n").map { "    $it" }
        return mypyCheckCode.joinToString("\n")
    }
}