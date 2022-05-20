package org.utbot.framework.codegen

import org.utbot.common.packageName
import org.utbot.framework.codegen.model.constructor.tree.TestsGenerationReport
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.UtMethod
import org.utbot.framework.plugin.api.UtTestCase

interface TestCodeGenerator {
    fun init(
        classUnderTest: Class<*>,
        params: MutableMap<UtMethod<*>, List<String>> = mutableMapOf(),
        testFramework: TestFramework = Junit5,
        mockFramework: MockFramework?,
        staticsMocking: StaticsMocking,
        forceStaticMocking: ForceStaticMocking,
        generateWarningsForStaticMocking: Boolean,
        codegenLanguage: CodegenLanguage = CodegenLanguage.JAVA,
        parameterizedTestSource: ParametrizedTestSource = ParametrizedTestSource.DO_NOT_PARAMETRIZE,
        runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.defaultItem,
        hangingTestsTimeout: HangingTestsTimeout = HangingTestsTimeout(),
        enableTestsTimeout: Boolean = true,
        testClassPackageName: String = classUnderTest.packageName
    )

    fun generateAsString(testCases: Collection<UtTestCase>, testClassCustomName: String? = null): String

    fun generateAsStringWithTestReport(
        testCases: Collection<UtTestCase>,
        testClassCustomName: String? = null
    ): TestsCodeWithTestReport
}

data class TestsCodeWithTestReport(val generatedCode: String, val testsGenerationReport: TestsGenerationReport)
