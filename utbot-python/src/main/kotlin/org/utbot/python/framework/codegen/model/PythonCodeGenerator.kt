package org.utbot.python.framework.codegen.model

import org.utbot.framework.codegen.*
import org.utbot.framework.codegen.model.CodeGenerator
import org.utbot.framework.codegen.model.TestsCodeWithTestReport
import org.utbot.framework.codegen.model.constructor.CgMethodTestSet
import org.utbot.framework.codegen.model.constructor.TestClassModel
import org.utbot.framework.codegen.model.constructor.context.CgContext
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.python.framework.codegen.PythonCodeLanguage
import org.utbot.python.framework.codegen.model.constructor.tree.PythonCgTestClassConstructor

class PythonCodeGenerator(
    classUnderTest: ClassId,
    paramNames: MutableMap<ExecutableId, List<String>> = mutableMapOf(),
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
    testClassPackageName: String = classUnderTest.packageName
) : CodeGenerator(
    classUnderTest,
    paramNames,
    testFramework,
    mockFramework,
    staticsMocking,
    forceStaticMocking,
    generateWarningsForStaticMocking,
    codegenLanguage,
    parameterizedTestSource,
    runtimeExceptionTestsBehaviour,
    hangingTestsTimeout,
    enableTestsTimeout,
    testClassPackageName
) {
    override var context: CgContext = CgContext(
        classUnderTest = classUnderTest,
        paramNames = paramNames,
        testFramework = testFramework,
        mockFramework = mockFramework ?: MockFramework.MOCKITO,
        codegenLanguage = codegenLanguage,
        codeGenLanguage = PythonCodeLanguage,
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
    ): TestsCodeWithTestReport = withCustomContext(testClassCustomName) {
        context.withTestClassFileScope {
            val testClassModel = TestClassModel(classUnderTest, cgTestSets)
            context.collectedImports.addAll(importModules)
            val testClassFile = PythonCgTestClassConstructor(context).construct(testClassModel)
            TestsCodeWithTestReport(renderClassFile(testClassFile), testClassFile.testsGenerationReport)
        }
    }
}