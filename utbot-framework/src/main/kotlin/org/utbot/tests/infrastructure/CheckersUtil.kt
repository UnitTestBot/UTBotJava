package org.utbot.tests.infrastructure

import org.utbot.framework.codegen.ForceStaticMocking
import org.utbot.framework.codegen.Junit4
import org.utbot.framework.codegen.MockitoStaticMocking
import org.utbot.framework.codegen.NoStaticMocking
import org.utbot.framework.codegen.ParametrizedTestSource
import org.utbot.framework.codegen.RuntimeExceptionTestsBehaviour
import org.utbot.framework.codegen.StaticsMocking
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi
import org.utbot.framework.plugin.api.MockStrategyApi.NO_MOCKS
import org.utbot.framework.plugin.api.MockStrategyApi.OTHER_CLASSES
import org.utbot.framework.util.Conflict
import org.utbot.framework.util.ConflictTriggers

data class TestFrameworkConfiguration(
    val testFramework: TestFramework,
    val mockFramework: MockFramework,
    val mockStrategy: MockStrategyApi,
    val staticsMocking: StaticsMocking,
    val parametrizedTestSource: ParametrizedTestSource,
    val codegenLanguage: CodegenLanguage,
    val forceStaticMocking: ForceStaticMocking,
    val resetNonFinalFieldsAfterClinit: Boolean = true,
    val generateUtilClassFile: Boolean,
    val runtimeExceptionTestsBehaviour: RuntimeExceptionTestsBehaviour = RuntimeExceptionTestsBehaviour.PASS,
    val enableTestsTimeout: Boolean = false // our tests should not fail due to timeout
) {
    val isParametrizedAndMocked: Boolean
        get() = parametrizedTestSource == ParametrizedTestSource.PARAMETRIZE &&
                (mockStrategy != NO_MOCKS ||
                        conflictTriggers[Conflict.ForceMockHappened] ?: false || conflictTriggers[Conflict.ForceStaticMockHappened] ?: false)

    val isDisabled: Boolean
        get() = run {
            // TODO Any? JIRA:1366
            if (codegenLanguage == CodegenLanguage.KOTLIN) return true

            // TODO a problem with try-with-resources JIRA:1329
            if (codegenLanguage == CodegenLanguage.KOTLIN && staticsMocking == MockitoStaticMocking) return true

            // TODO There is no assertArrayEquals JIRA:1416
            if (testFramework == TestNg) return true

            // because otherwise the code generator will not create mocks even for mandatory to mock classes
            if (forceStaticMocking == ForceStaticMocking.FORCE && staticsMocking == NoStaticMocking) return true

            // junit4 doesn't support parametrized tests
            if (testFramework == Junit4 && parametrizedTestSource == ParametrizedTestSource.PARAMETRIZE) return true

            // if we want to generate mocks for every class but CUT, we must have specified staticsMocking
            if (mockStrategy == OTHER_CLASSES && staticsMocking == NoStaticMocking) return true

            return false
        }
}

val conflictTriggers: ConflictTriggers = ConflictTriggers()

val allTestFrameworkConfigurations: List<TestFrameworkConfiguration> = run {
    val possibleConfiguration = mutableListOf<TestFrameworkConfiguration>()

    for (mockStrategy in listOf(NO_MOCKS, OTHER_CLASSES)) {
        for (testFramework in TestFramework.allItems) {
            val mockFramework = MockFramework.MOCKITO
            val forceStaticMocking = ForceStaticMocking.FORCE

            for (staticsMocking in StaticsMocking.allItems) {
                for (parametrizedTestSource in ParametrizedTestSource.allItems) {
                    for (codegenLanguage in CodegenLanguage.allItems) {
                        // We should not reset values for non-final static fields in parameterized tests
                        val resetNonFinalFieldsAfterClinit =
                            parametrizedTestSource == ParametrizedTestSource.DO_NOT_PARAMETRIZE

                        possibleConfiguration += TestFrameworkConfiguration(
                            testFramework,
                            mockFramework,
                            mockStrategy,
                            staticsMocking,
                            parametrizedTestSource,
                            codegenLanguage,
                            forceStaticMocking,
                            resetNonFinalFieldsAfterClinit,
                            generateUtilClassFile = false
                        )
                        possibleConfiguration += TestFrameworkConfiguration(
                            testFramework,
                            mockFramework,
                            mockStrategy,
                            staticsMocking,
                            parametrizedTestSource,
                            codegenLanguage,
                            forceStaticMocking,
                            resetNonFinalFieldsAfterClinit,
                            generateUtilClassFile = true
                        )
                    }
                }
            }
        }
    }

    possibleConfiguration.toList()
}