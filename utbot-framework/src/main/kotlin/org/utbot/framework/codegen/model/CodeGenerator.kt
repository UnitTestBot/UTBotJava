package org.utbot.framework.codegen.model

import org.utbot.common.packageName
import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.HangingTestsTimeout
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.CgTestClassConstructor
import org.utbot.framework.codegen.model.constructor.tree.TestsGenerationReport
import org.utbot.framework.codegen.model.tree.CgTestClassFile
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtMethodTestSet
import org.utbot.framework.plugin.api.util.id

class CodeGenerator(
    private val classUnderTest: Class<*>,
    params: MutableMap<UtMethod<*>, List<String>> = mutableMapOf(),
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
        classUnderTest = classUnderTest.id,
        paramNames = params,
        testFramework = testFramework,
        mockFramework = mockFramework ?: MockFramework.MOCKITO,
        codegenLanguage = codegenLanguage,
        parameterizedTestSource = parameterizedTestSource,
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
    ): TestsCodeWithTestReport =
            withCustomContext(testClassCustomName) {
                context.withClassScope {
                    val testClassFile = CgTestClassConstructor(context).construct(testSets)
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

