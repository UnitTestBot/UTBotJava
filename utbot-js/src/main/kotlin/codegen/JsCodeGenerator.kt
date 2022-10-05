package codegen

import framework.codegen.JsCodeLanguage
import framework.codegen.Mocha
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.HangingTestsTimeout
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RegularImport
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.model.TestsCodeWithTestReport
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.codegen.model.constructor.TestClassModel
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.CgTestClassConstructor
import org.utbot.framework.codegen.model.tree.CgTestClassFile
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.js.JsClassId
import settings.JsTestGenerationSettings.fileUnderTestAliases

class JsCodeGenerator(
    private val classUnderTest: JsClassId,
    paramNames: MutableMap<ExecutableId, List<String>> = mutableMapOf(),
    testFramework: TestFramework = Mocha,
    runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.defaultItem,
    hangingTestsTimeout: HangingTestsTimeout = HangingTestsTimeout(),
    enableTestsTimeout: Boolean = true,
    testClassPackageName: String = classUnderTest.packageName,
    importPrefix: String,
) {
    private var context: CgContext = CgContext(
        classUnderTest = classUnderTest,
        paramNames = paramNames,
        testFramework = testFramework,
        mockFramework = MockFramework.MOCKITO,
        codegenLanguage = CodegenLanguage.JS,
        codeGenLanguage = JsCodeLanguage,
        parametrizedTestSource = ParametrizedTestSource.defaultItem,
        staticsMocking = StaticsMocking.defaultItem,
        forceStaticMocking = ForceStaticMocking.defaultItem,
        generateWarningsForStaticMocking = true,
        runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour,
        hangingTestsTimeout = hangingTestsTimeout,
        enableTestsTimeout = enableTestsTimeout,
        testClassPackageName = testClassPackageName,
        collectedImports = mutableSetOf(
            RegularImport("assert", "assert"),
            RegularImport(
                fileUnderTestAliases,
                "./$importPrefix/${classUnderTest.filePath.substringAfterLast("/")}"
            )
        )
    )

    fun generateAsStringWithTestReport(
        cgTestSets: List<CgMethodTestSet>,
        testClassCustomName: String? = null,
    ): TestsCodeWithTestReport = withCustomContext(testClassCustomName) {
        val testClassModel = TestClassModel(classUnderTest, cgTestSets)
        val testClassFile = CgTestClassConstructor(context).construct(testClassModel)
        TestsCodeWithTestReport(renderClassFile(testClassFile), testClassFile.testsGenerationReport)
    }

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