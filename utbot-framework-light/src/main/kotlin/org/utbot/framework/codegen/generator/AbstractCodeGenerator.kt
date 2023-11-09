package org.utbot.framework.codegen.generator

import mu.KotlinLogging
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgClassFile
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.plugin.api.UtMethodTestSet
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class AbstractCodeGenerator(params: CodeGeneratorParams) {
    protected val logger = KotlinLogging.logger {}

    open var context: CgContext = with(params) {
        CgContext(
            classUnderTest = classUnderTest,
            projectType = projectType,
            generateUtilClassFile = generateUtilClassFile,
            paramNames = paramNames,
            testFramework = testFramework,
            mockFramework = mockFramework,
            codegenLanguage = codegenLanguage,
            cgLanguageAssistant = cgLanguageAssistant,
            parametrizedTestSource = parameterizedTestSource,
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
                generate(cgTestSets)
            }
        }
    }

    protected abstract fun generate(testSets: List<CgMethodTestSet>): CodeGeneratorResult

    protected fun renderToString(testClassFile: CgClassFile): String {
        logger.info { "Rendering phase started at ${now()}" }
        val renderer = CgAbstractRenderer.makeRenderer(context)
        testClassFile.accept(renderer)
        logger.info { "Rendering phase finished at ${now()}" }

        return renderer.toString()
    }

    protected fun now(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))

    /**
     * Wrapper function that configures context as needed for utbot-online:
     * - turns on imports optimization in code generator
     * - passes a custom test class name if there is one
     */
    fun <R> withCustomContext(testClassCustomName: String? = null, block: () -> R): R {
        val prevContext = context
        return try {
            context = prevContext.customCopy(shouldOptimizeImports = true, testClassCustomName = testClassCustomName)
            block()
        } finally {
            context = prevContext
        }
    }
}