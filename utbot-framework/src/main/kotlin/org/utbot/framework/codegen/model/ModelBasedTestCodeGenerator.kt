package org.utbot.framework.codegen.model

import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.HangingTestsTimeout
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestCodeGenerator
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.TestsCodeWithTestReport
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.codegen.model.constructor.tree.CgTestClassConstructor
import org.utbot.framework.codegen.model.tree.CgTestClassFile
import org.utbot.framework.codegen.model.visitor.CgAbstractRenderer
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtTestCase
import org.utbot.framework.plugin.api.util.id

class ModelBasedTestCodeGenerator : TestCodeGenerator {
    private lateinit var context: CgContext

    override fun init(
        classUnderTest: Class<*>,
        params: MutableMap<UtMethod<*>, List<String>>,
        testFramework: TestFramework,
        mockFramework: MockFramework?,
        staticsMocking: StaticsMocking,
        forceStaticMocking: ForceStaticMocking,
        generateWarningsForStaticMocking: Boolean,
        codegenLanguage: CodegenLanguage,
        parameterizedTestSource: ParametrizedTestSource,
        runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour,
        hangingTestsTimeout: HangingTestsTimeout,
        enableTestsTimeout: Boolean,
        testClassPackageName: String
    ) {
        context = CgContext(
            classUnderTest = classUnderTest.id,
            // TODO: remove existingNames parameter completely
            existingMethodNames = mutableSetOf(),
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
    }

    //TODO: we support custom test class name only in utbot-online, probably support them in plugin as well
    override fun generateAsString(testCases: Collection<UtTestCase>, testClassCustomName: String?): String =
        generateAsStringWithTestReport(testCases, testClassCustomName).generatedCode

    //TODO: we support custom test class name only in utbot-online, probably support them in plugin as well
    override fun generateAsStringWithTestReport(
        testCases: Collection<UtTestCase>,
        testClassCustomName: String?
    ): TestsCodeWithTestReport =
            withCustomContext(testClassCustomName) {
                context.withClassScope {
                    val testClassFile = CgTestClassConstructor(context).construct(testCases)
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
