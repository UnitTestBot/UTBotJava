package org.utbot.framework.codegen.generator

import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.builders.SimpleTestClassModelBuilder
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.framework.codegen.tree.CgSimpleTestClassConstructor
import org.utbot.framework.codegen.tree.ututils.UtilClassKind
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework

open class CodeGenerator(
    val classUnderTest: ClassId,
    val projectType: ProjectType,
    paramNames: MutableMap<ExecutableId, List<String>> = mutableMapOf(),
    generateUtilClassFile: Boolean = false,
    testFramework: TestFramework = TestFramework.defaultItem,
    mockFramework: MockFramework = MockFramework.defaultItem,
    staticsMocking: StaticsMocking = StaticsMocking.defaultItem,
    forceStaticMocking: ForceStaticMocking = ForceStaticMocking.defaultItem,
    generateWarningsForStaticMocking: Boolean = true,
    codegenLanguage: CodegenLanguage = CodegenLanguage.defaultItem,
    cgLanguageAssistant: CgLanguageAssistant = CgLanguageAssistant.getByCodegenLanguage(codegenLanguage),
    parameterizedTestSource: ParametrizedTestSource = ParametrizedTestSource.defaultItem,
    runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.defaultItem,
    hangingTestsTimeout: HangingTestsTimeout = HangingTestsTimeout(),
    enableTestsTimeout: Boolean = true,
    testClassPackageName: String = classUnderTest.packageName,
): AbstractCodeGenerator(
    classUnderTest,
    projectType,
    paramNames,
    generateUtilClassFile,
    testFramework,
    mockFramework,
    staticsMocking,
    forceStaticMocking,
    generateWarningsForStaticMocking,
    codegenLanguage,
    cgLanguageAssistant,
    parameterizedTestSource,
    runtimeExceptionTestsBehaviour,
    hangingTestsTimeout,
    enableTestsTimeout,
    testClassPackageName,
) {

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

