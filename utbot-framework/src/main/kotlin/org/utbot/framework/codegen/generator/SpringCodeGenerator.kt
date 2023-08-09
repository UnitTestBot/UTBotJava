package org.utbot.framework.codegen.generator

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.builders.SimpleTestClassModelBuilder
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.framework.codegen.tree.CgCustomAssertConstructor
import org.utbot.framework.codegen.tree.CgSpringIntegrationTestClassConstructor
import org.utbot.framework.codegen.tree.CgSpringUnitTestClassConstructor
import org.utbot.framework.codegen.tree.CgSpringVariableConstructor
import org.utbot.framework.codegen.tree.CgVariableConstructor
import org.utbot.framework.codegen.tree.ututils.UtilClassKind
import org.utbot.framework.codegen.tree.withCustomAssertForMockMvcResultActions
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ConcreteContextLoadingResult
import org.utbot.framework.plugin.api.SpringSettings
import org.utbot.framework.plugin.api.SpringSettings.AbsentSpringSettings
import org.utbot.framework.plugin.api.SpringSettings.PresentSpringSettings
import org.utbot.framework.plugin.api.SpringTestType

class SpringCodeGenerator(
    private val springTestType: SpringTestType,
    private val springSettings: SpringSettings,
    private val concreteContextLoadingResult: ConcreteContextLoadingResult?,
    params: CodeGeneratorParams
) : AbstractCodeGenerator(
    params.copy(
        cgLanguageAssistant = object : CgLanguageAssistant by params.cgLanguageAssistant {
            override fun getVariableConstructorBy(context: CgContext): CgVariableConstructor =
                // TODO decorate original `params.cgLanguageAssistant.getVariableConstructorBy(context)`
                CgSpringVariableConstructor(context)

            override fun getCustomAssertConstructorBy(context: CgContext): CgCustomAssertConstructor =
                params.cgLanguageAssistant.getCustomAssertConstructorBy(context)
                    .withCustomAssertForMockMvcResultActions()
        }
    )
) {
    private val classUnderTest: ClassId = params.classUnderTest

    override fun generate(testSets: List<CgMethodTestSet>): CodeGeneratorResult {
        val testClassModel = SimpleTestClassModelBuilder().createTestClassModel(classUnderTest, testSets)

        logger.info { "Code generation phase started at ${now()}" }
        val astConstructor = when (springTestType) {
            SpringTestType.UNIT_TEST -> CgSpringUnitTestClassConstructor(context)
            SpringTestType.INTEGRATION_TEST ->
                when (val settings = springSettings) {
                    is PresentSpringSettings -> CgSpringIntegrationTestClassConstructor(context, concreteContextLoadingResult, settings)
                    is AbsentSpringSettings -> error("No Spring settings were provided for Spring integration test generation.")
                }
        }
        val testClassFile = astConstructor.construct(testClassModel)
        logger.info { "Code generation phase finished at ${now()}" }

        val generatedCode = renderToString(testClassFile)

        return CodeGeneratorResult(
            generatedCode = generatedCode,
            utilClassKind = UtilClassKind.fromCgContextOrNull(context),
            testsGenerationReport = astConstructor.testsGenerationReport,
        )
    }
}