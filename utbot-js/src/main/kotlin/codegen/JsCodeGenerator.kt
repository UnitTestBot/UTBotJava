package codegen

import framework.codegen.JsCgLanguageAssistant
import framework.codegen.Mocha
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework
import framework.api.js.JsClassId
import org.utbot.framework.codegen.CodeGeneratorResult
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.RegularImport
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgClassFile
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.codegen.tree.CgSimpleTestClassConstructor
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
        codegenLanguage = CodegenLanguage.defaultItem,
        cgLanguageAssistant = JsCgLanguageAssistant,
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
    ): CodeGeneratorResult = withCustomContext(testClassCustomName) {
        val testClassModel = TestClassModel(classUnderTest, cgTestSets)
        val astConstructor = CgSimpleTestClassConstructor(context)
        val testClassFile = astConstructor.construct(testClassModel)
        CodeGeneratorResult(renderClassFile(testClassFile), astConstructor.testsGenerationReport)
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

    private fun renderClassFile(file: CgClassFile): String {
        val renderer = CgAbstractRenderer.makeRenderer(context)
        file.accept(renderer)
        return renderer.toString()
    }
}