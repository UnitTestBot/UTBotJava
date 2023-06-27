package org.utbot.python.framework.codegen.model

import org.utbot.framework.codegen.generator.CodeGeneratorResult
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.SimpleTestClassModel
import org.utbot.framework.codegen.generator.CodeGenerator
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.codegen.renderer.CgPrinterImpl
import org.utbot.framework.codegen.renderer.CgRendererContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.python.PythonMethod
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.framework.codegen.model.constructor.tree.PythonCgTestClassConstructor
import org.utbot.python.framework.codegen.model.constructor.visitor.CgPythonRenderer
import org.utbot.python.newtyping.general.Type
import org.utbot.python.newtyping.pythonAnyType
import org.utbot.python.newtyping.pythonModules
import org.utbot.python.newtyping.pythonTypeRepresentation

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
    classUnderTest = classUnderTest,
    projectType = ProjectType.Python,
    paramNames = paramNames,
    generateUtilClassFile = true,
    testFramework = testFramework,
    mockFramework = mockFramework,
    staticsMocking = staticsMocking,
    forceStaticMocking = forceStaticMocking,
    generateWarningsForStaticMocking = generateWarningsForStaticMocking,
    parameterizedTestSource = parameterizedTestSource,
    runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour,
    hangingTestsTimeout = hangingTestsTimeout,
    enableTestsTimeout = enableTestsTimeout,
    testClassPackageName = testClassPackageName,
    cgLanguageAssistant = PythonCgLanguageAssistant,
) {
    fun pythonGenerateAsStringWithTestReport(
        cgTestSets: List<CgMethodTestSet>,
        importModules: Set<PythonImport>,
        testClassCustomName: String? = null,
    ): CodeGeneratorResult = withCustomContext(testClassCustomName) {
        context.withTestClassFileScope {
            (context.cgLanguageAssistant as PythonCgLanguageAssistant).clear()
            val testClassModel = SimpleTestClassModel(classUnderTest, cgTestSets)
            context.collectedImports.addAll(importModules)

            val astConstructor = PythonCgTestClassConstructor(context)
            val renderer = CgAbstractRenderer.makeRenderer(context)

            val testClassFile = astConstructor.construct(testClassModel)
            testClassFile.accept(renderer)

            CodeGeneratorResult(renderer.toString(), astConstructor.testsGenerationReport)
        }
    }

    fun generateMypyCheckCode(
        method: PythonMethod,
        methodAnnotations: Map<String, Type>,
        directoriesForSysPath: Set<String>,
        moduleToImport: String,
        namesInModule: Collection<String>,
        additionalVars: String
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

        val additionalModules = methodAnnotations.values.flatMap { it.pythonModules() }.map { PythonUserImport(it) }
        val imports =
            (listOf(importSys, importTyping) + importSysPaths + (importsFromModule + additionalModules)).toSet()
                .toList()

        imports.forEach { renderer.renderPythonImport(it) }

        val paramNames = method.definition.meta.args.map { it.name }
        val parameters = paramNames.map { argument ->
            "${argument}: ${methodAnnotations[argument]?.pythonTypeRepresentation() ?: pythonAnyType.pythonTypeRepresentation()}"
        }

        val functionPrefix = "__mypy_check"
        val functionName =
            "def ${functionPrefix}_${method.name}(${parameters.joinToString(", ")}):"  // TODO: in future can be "async def"

        val mypyCheckCode = listOf(
            renderer.toString(),
            "",
            additionalVars,
            "",
            functionName,
        ) + method.codeAsString.split("\n").map { "    $it" }
        return mypyCheckCode.joinToString("\n")
    }
}