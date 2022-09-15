package org.utbot.framework.codegen.model

import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.CgTestClassConstructor
import org.utbot.framework.codegen.model.constructor.tree.TestsGenerationReport
import org.utbot.framework.codegen.model.tree.CgTestClassFile
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.plugin.api.*
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.codegen.model.constructor.TestClassModel
import org.utbot.framework.codegen.model.constructor.tree.PythonCgTestClassConstructor

class CodeGenerator(
    val classUnderTest: ClassId,
    paramNames: MutableMap<ExecutableId, List<String>> = mutableMapOf(),
    testFramework: TestFramework = TestFramework.defaultItem,
    mockFramework: MockFramework? = MockFramework.defaultItem,
    staticsMocking: StaticsMocking = StaticsMocking.defaultItem,
    forceStaticMocking: ForceStaticMocking = ForceStaticMocking.defaultItem,
    generateWarningsForStaticMocking: Boolean = true,
    codegenLanguage: CodegenLanguage = CodegenLanguage.defaultItem,
    parameterizedTestSource: ParametrizedTestSource = ParametrizedTestSource.defaultItem,
    runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.defaultItem,
    hangingTestsTimeout: HangingTestsTimeout = HangingTestsTimeout(),
    enableTestsTimeout: Boolean = true,
    testClassPackageName: String = classUnderTest.packageName,
) {
    private var context: CgContext = CgContext(
        classUnderTest = classUnderTest,
        paramNames = paramNames,
        testFramework = testFramework,
        mockFramework = mockFramework ?: MockFramework.MOCKITO,
        codegenLanguage = codegenLanguage,
        parametrizedTestSource = parameterizedTestSource,
        staticsMocking = staticsMocking,
        forceStaticMocking = forceStaticMocking,
        generateWarningsForStaticMocking = generateWarningsForStaticMocking,
        runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour,
        hangingTestsTimeout = hangingTestsTimeout,
        enableTestsTimeout = enableTestsTimeout,
        testClassPackageName = testClassPackageName
    )

    //TODO: we support custom test class name only in utbot-online, probably support them in plugin as well
    fun generateAsString(testSets: Collection<UtMethodTestSet>, testClassCustomName: String? = null): String =
        generateAsStringWithTestReport(testSets, testClassCustomName).generatedCode

    //TODO: we support custom test class name only in utbot-online, probably support them in plugin as well
    fun generateAsStringWithTestReport(
        testSets: Collection<UtMethodTestSet>,
        testClassCustomName: String? = null,
    ): TestsCodeWithTestReport {
        val cgTestSets = testSets.map { CgMethodTestSet(it) }.toList()
        return generateAsStringWithTestReport(cgTestSets, testClassCustomName)
    }

    fun generateAsStringWithTestReport(
        cgTestSets: List<CgMethodTestSet>,
        testClassCustomName: String? = null,
    ): TestsCodeWithTestReport = withCustomContext(testClassCustomName) {
        context.withTestClassFileScope {
            val testClassModel = TestClassModel.fromTestSets(classUnderTest, cgTestSets)
            val testClassFile = CgTestClassConstructor(context).construct(testClassModel)
            TestsCodeWithTestReport(renderClassFile(testClassFile), testClassFile.testsGenerationReport)
        }
    }

    fun pythonGenerateAsStringWithTestReport(
        cgTestSets: List<CgMethodTestSet>,
        importModules: Set<PythonImport>,
        testClassCustomName: String? = null,
    ): TestsCodeWithTestReport = withCustomContext(testClassCustomName) {
        context.withTestClassFileScope {
            val testClassModel = TestClassModel.fromTestSets(classUnderTest, cgTestSets)
            context.collectedImports.addAll(importModules)
            val testClassFile = PythonCgTestClassConstructor(context).construct(testClassModel)
            TestsCodeWithTestReport(renderClassFile(testClassFile), testClassFile.testsGenerationReport)
        }
    }

    /**
     * Wrapper function that configures context as needed for utbot-online:
     * - turns on imports optimization in code generator
     * - passes a custom test class name if there is one
     */
    private fun <R> withCustomContext(testClassCustomName: String? = null, block: () -> R): R {
        val prevContext = context
        return try {
            context = prevContext.copy(
                    shouldOptimizeImports = true,
                    testClassCustomName = testClassCustomName
            )
            block()
        } finally {
            context = prevContext
        }
    }

    private fun renderClassFile(file: CgTestClassFile): String {
        val renderer = CgAbstractRenderer.makeRenderer(context)
        file.accept(renderer)
        return renderer.toString()
    }
}

data class TestsCodeWithTestReport(val generatedCode: String, val testsGenerationReport: TestsGenerationReport)

