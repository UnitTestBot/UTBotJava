package org.utbot.framework.codegen.generator

import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.builders.SimpleTestClassModelBuilder
import org.utbot.framework.codegen.tree.CgSimpleTestClassConstructor
import org.utbot.framework.codegen.tree.ututils.UtilClassKind
import org.utbot.framework.plugin.api.ClassId

open class CodeGenerator(params: CodeGeneratorParams): AbstractCodeGenerator(params) {
    val classUnderTest: ClassId = params.classUnderTest

    override fun generate(testSets: List<CgMethodTestSet>): CodeGeneratorResult {
        val testClassModel = SimpleTestClassModelBuilder(context).createTestClassModel(classUnderTest, testSets)

        logger.info { "Code generation phase started at ${now()}" }
        val astConstructor = CgSimpleTestClassConstructor(context)
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

