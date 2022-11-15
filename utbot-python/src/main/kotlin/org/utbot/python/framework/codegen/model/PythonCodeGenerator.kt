package org.utbot.python.framework.codegen.model

import org.utbot.framework.codegen.CodeGenerator
import org.utbot.framework.codegen.CodeGeneratorResult
import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.PythonImport
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.models.CgMethodTestSet
import org.utbot.framework.codegen.domain.models.TestClassModel
import org.utbot.framework.codegen.domain.context.CgContext
import org.utbot.framework.codegen.renderer.CgAbstractRenderer
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.python.framework.codegen.PythonCgLanguageAssistant
import org.utbot.python.framework.codegen.model.constructor.tree.PythonCgTestClassConstructor

class PythonCodeGenerator(
    classUnderTest: ClassId,
    paramNames: MutableMap<ExecutableId, List<String>> = mutableMapOf(),
    testFramework: TestFramework = TestFramework.defaultItem,
    mockFramework: MockFramework = MockFramework.defaultItem,
    staticsMocking: StaticsMocking = StaticsMocking.defaultItem,
    forceStaticMocking: ForceStaticMocking = ForceStaticMocking.defaultItem,
    generateWarningsForStaticMocking: Boolean = true,
    parameterizedTestSource: ParametrizedTestSource = ParametrizedTestSource.defaultItem,
    runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.defaultItem,
    hangingTestsTimeout: HangingTestsTimeout = HangingTestsTimeout(),
    enableTestsTimeout: Boolean = true,
    testClassPackageName: String = classUnderTest.packageName
) : CodeGenerator(
    classUnderTest=classUnderTest,
    paramNames=paramNames,
    generateUtilClassFile = true,
    testFramework=testFramework,
    mockFramework=mockFramework,
    staticsMocking=staticsMocking,
    forceStaticMocking=forceStaticMocking,
    generateWarningsForStaticMocking=generateWarningsForStaticMocking,
    parameterizedTestSource=parameterizedTestSource,
    runtimeExceptionTestsBehaviour=runtimeExceptionTestsBehaviour,
    hangingTestsTimeout=hangingTestsTimeout,
    enableTestsTimeout=enableTestsTimeout,
    testClassPackageName=testClassPackageName
) {
    override var context: CgContext = CgContext(
        classUnderTest = classUnderTest,
        paramNames = paramNames,
        testFramework = testFramework,
        mockFramework = mockFramework,
        cgLanguageAssistant = PythonCgLanguageAssistant,
        parametrizedTestSource = parameterizedTestSource,
        staticsMocking = staticsMocking,
        forceStaticMocking = forceStaticMocking,
        generateWarningsForStaticMocking = generateWarningsForStaticMocking,
        runtimeExceptionTestsBehaviour = runtimeExceptionTestsBehaviour,
        hangingTestsTimeout = hangingTestsTimeout,
        enableTestsTimeout = enableTestsTimeout,
        testClassPackageName = testClassPackageName
    )

    fun pythonGenerateAsStringWithTestReport(
        cgTestSets: List<CgMethodTestSet>,
        importModules: Set<PythonImport>,
        testClassCustomName: String? = null,
    ): CodeGeneratorResult = withCustomContext(testClassCustomName) {
        context.withTestClassFileScope {
            val testClassModel = TestClassModel(classUnderTest, cgTestSets)
            context.collectedImports.addAll(importModules)

            val astConstructor = PythonCgTestClassConstructor(context)
            val renderer = CgAbstractRenderer.makeRenderer(context)

            val testClassFile = astConstructor.construct(testClassModel)
            testClassFile.accept(renderer)

            CodeGeneratorResult(renderer.toString(), astConstructor.testsGenerationReport)
        }
    }
}