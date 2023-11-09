package org.utbot.framework.codegen.generator

import org.utbot.framework.codegen.domain.ForceStaticMocking
import org.utbot.framework.codegen.domain.HangingTestsTimeout
import org.utbot.framework.codegen.domain.ParametrizedTestSource
import org.utbot.framework.codegen.domain.ProjectType
import org.utbot.framework.codegen.domain.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.domain.StaticsMocking
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.services.language.CgLanguageAssistant
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.ExecutableId
import org.utbot.framework.plugin.api.MockFramework

data class CodeGeneratorParams(
    val classUnderTest: ClassId,
    val projectType: ProjectType,
    val paramNames: MutableMap<ExecutableId, List<String>> = mutableMapOf(),
    val generateUtilClassFile: Boolean = false,
    val testFramework: TestFramework = TestFramework.defaultItem,
    val mockFramework: MockFramework = MockFramework.defaultItem,
    val staticsMocking: StaticsMocking = StaticsMocking.defaultItem,
    val forceStaticMocking: ForceStaticMocking = ForceStaticMocking.defaultItem,
    val generateWarningsForStaticMocking: Boolean = true,
    val codegenLanguage: CodegenLanguage = CodegenLanguage.defaultItem,
    val cgLanguageAssistant: CgLanguageAssistant = CgLanguageAssistant.getByCodegenLanguage(codegenLanguage),
    val parameterizedTestSource: ParametrizedTestSource = ParametrizedTestSource.defaultItem,
    val runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.defaultItem,
    val hangingTestsTimeout: HangingTestsTimeout = HangingTestsTimeout(),
    val enableTestsTimeout: Boolean = true,
    val testClassPackageName: String = classUnderTest.packageName,
)