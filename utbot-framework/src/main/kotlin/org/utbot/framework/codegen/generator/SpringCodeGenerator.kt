package org.utbot.framework.codegen.generator

import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.builders.SpringTestClassModelBuilder
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.framework.codegen.tree.CgSpringIntegrationTestClassConstructor
import org.utbot.framework.codegen.tree.CgSpringUnitTestClassConstructor
import org.utbot.framework.codegen.tree.CgSpringVariableConstructor
import org.utbot.framework.codegen.tree.CgVariableConstructor
import org.utbot.framework.codegen.tree.ututils.UtilClassKind
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.SpringCodeGenerationContext
import org.utbot.framework.plugin.api.SpringSettings.AbsentSpringSettings
import org.utbot.framework.plugin.api.SpringSettings.PresentSpringSettings
import org.utbot.framework.plugin.api.SpringTestType

class SpringCodeGenerator(
    val springCodeGenerationContext: SpringCodeGenerationContext,
    params: CodeGeneratorParams
) : AbstractCodeGenerator(
    params.copy(
        cgLanguageAssistant = object : CgLanguageAssistant by params.cgLanguageAssistant {
            override fun getVariableConstructorBy(context: CgContext): CgVariableConstructor =
                // TODO decorate original `params.cgLanguageAssistant.getVariableConstructorBy(context)`
                CgSpringVariableConstructor(context)
        }
    )
) {
    val classUnderTest: ClassId = params.classUnderTest

    override fun generate(testSets: List<CgMethodTestSet>): CodeGeneratorResult {
        val testClassModel = SpringTestClassModelBuilder(context).createTestClassModel(classUnderTest, testSets)

        logger.info { "Code generation phase started at ${now()}" }
        val astConstructor = when (springCodeGenerationContext.springTestType) {
            SpringTestType.UNIT_TEST -> CgSpringUnitTestClassConstructor(context)
            SpringTestType.INTEGRATION_TEST ->
                when (val settings = springCodeGenerationContext.springSettings) {
                    is PresentSpringSettings -> CgSpringIntegrationTestClassConstructor(context, springCodeGenerationContext, settings)
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