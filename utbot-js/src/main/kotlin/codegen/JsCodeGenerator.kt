package codegen

import framework.api.js.JsClassId
import framework.codegen.JsCgLanguageAssistant
import framework.codegen.JsImport
import framework.codegen.Mocha
import org.utbot.framework.codegen.CodeGeneratorResult
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgClassFile
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.SimpleTestClassModel
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.codegen.tree.CgSimpleTestClassConstructor
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework

class JsCodeGenerator(
    private val classUnderTest: JsClassId,
    paramNames: MutableMap<ExecutableId, List<String>> = mutableMapOf(),
    testFramework: TestFramework = Mocha,
    runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.defaultItem,
    hangingTestsTimeout: HangingTestsTimeout = HangingTestsTimeout(),
    enableTestsTimeout: Boolean = true,
    testClassPackageName: String = classUnderTest.packageName,
    imports: List<JsImport>,
) {
    private var context: CgContext = CgContext(
        classUnderTest = classUnderTest,
        projectType = ProjectType.JavaScript,
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
        collectedImports = imports.toMutableSet()
    )

    fun generateAsStringWithTestReport(
        cgTestSets: List<CgMethodTestSet>,
        testClassCustomName: String? = null,
    ): CodeGeneratorResult = withCustomContext(testClassCustomName) {
        val testClassModel = SimpleTestClassModel(classUnderTest, cgTestSets)
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
