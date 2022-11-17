package org.utbot.framework.codegen

import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.codegen.reports.TestsGenerationReport
import org.utbot.framework.codegen.tree.CgTestClassConstructor
import org.utbot.framework.codegen.tree.ututils.UtilClassKind
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.UtMethodTestSet

open class CodeGenerator(
    val classUnderTest: ClassId,
    paramNames: MutableMap<ExecutableId, List<String>> = mutableMapOf(),
    generateUtilClassFile: Boolean = false,
    testFramework: TestFramework = TestFramework.defaultItem,
    mockFramework: MockFramework = MockFramework.defaultItem,
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

    open var context: CgContext = CgContext(
        classUnderTest = classUnderTest,
        generateUtilClassFile = generateUtilClassFile,
        paramNames = paramNames,
        testFramework = testFramework,
        mockFramework = mockFramework,
        codegenLanguage = codegenLanguage,
        cgLanguageAssistant = CgLanguageAssistant.getByCodegenLanguage(codegenLanguage),
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
    ): CodeGeneratorResult {
        val cgTestSets = testSets.map { CgMethodTestSet(it) }.toList()
        return withCustomContext(testClassCustomName) {
            context.withTestClassFileScope {
                val astConstructor = CgTestClassConstructor(context)
                val renderer = CgAbstractRenderer.makeRenderer(context)
                val testClassModel = TestClassModel.fromTestSets(classUnderTest, cgTestSets)

                val testClassFile = astConstructor.construct(testClassModel)
                testClassFile.accept(renderer)

                CodeGeneratorResult(
                    generatedCode = renderer.toString(),
                    utilClassKind = UtilClassKind.fromCgContextOrNull(context),
                    testsGenerationReport = astConstructor.testsGenerationReport
                )
            }
        }
    }

    /**
     * Wrapper function that configures context as needed for utbot-online:
     * - turns on imports optimization in code generator
     * - passes a custom test class name if there is one
     */
    fun <R> withCustomContext(testClassCustomName: String? = null, block: () -> R): R {
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
}

/**
 * @property generatedCode the source code of the test class
 * @property testsGenerationReport some info about test generation process
 * @property utilClassKind the kind of util class if it is required, otherwise - null
 */
data class CodeGeneratorResult(
    val generatedCode: String,
    val testsGenerationReport: TestsGenerationReport,
    // null if no util class needed, e.g. when we are generating utils directly into test class
    val utilClassKind: UtilClassKind? = null,
)

