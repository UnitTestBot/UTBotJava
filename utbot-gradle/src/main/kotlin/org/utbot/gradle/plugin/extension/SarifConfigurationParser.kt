package org.utbot.gradle.plugin.extension

import org.utbot.engine.Mocker
import org.utbot.framework.codegen.*
import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.CodegenLanguage
import org.utbot.framework.plugin.api.MockFramework
import org.utbot.framework.plugin.api.MockStrategyApi

object SarifConfigurationParser {

    fun testFrameworkParse(testFramework: String): TestFramework =
        when (testFramework.toLowerCase()) {
            "junit4" -> Junit4
            "junit5" -> Junit5
            "testng" -> TestNg
            else -> error("Parameter testFramework == '$testFramework', but it can take only 'junit4', 'junit5' or 'testng'")
        }

    fun mockFrameworkParse(mockFramework: String): MockFramework =
        when (mockFramework.toLowerCase()) {
            "mockito" -> MockFramework.MOCKITO
            else -> error("Parameter mockFramework == '$mockFramework', but it can take only 'mockito'")
        }

    fun generationTimeoutParse(generationTimeout: Long): Long {
        if (generationTimeout < 0)
            error("Parameter generationTimeout == $generationTimeout, but it should be non-negative")
        return generationTimeout
    }

    fun codegenLanguageParse(codegenLanguage: String): CodegenLanguage =
        when (codegenLanguage.toLowerCase()) {
            "java" -> CodegenLanguage.JAVA
            "kotlin" -> CodegenLanguage.KOTLIN
            else -> error("Parameter codegenLanguage == '$codegenLanguage', but it can take only 'java' or 'kotlin'")
        }

    fun mockStrategyParse(mockStrategy: String): MockStrategyApi =
        when (mockStrategy.toLowerCase()) {
            "do-not-mock" -> MockStrategyApi.NO_MOCKS
            "package-based" -> MockStrategyApi.OTHER_PACKAGES
            "all-except-cut" -> MockStrategyApi.OTHER_CLASSES
            else -> error("Parameter mockStrategy == '$mockStrategy', but it can take only 'do-not-mock', 'package-based' or 'all-except-cut'")
        }

    fun staticsMockingParse(staticsMocking: String): StaticsMocking =
        when (staticsMocking.toLowerCase()) {
            "do-not-mock-statics" -> NoStaticMocking
            "mock-statics" -> MockitoStaticMocking
            else -> error("Parameter staticsMocking == '$staticsMocking', but it can take only 'do-not-mock-statics' or 'mock-statics'")
        }

    fun forceStaticMockingParse(forceStaticMocking: String): ForceStaticMocking =
        when (forceStaticMocking.toLowerCase()) {
            "force" -> ForceStaticMocking.FORCE
            "do-not-force" -> ForceStaticMocking.DO_NOT_FORCE
            else -> error("Parameter forceStaticMocking == '$forceStaticMocking', but it can take only 'force' or 'do-not-force'")
        }

    fun classesToMockAlwaysParse(specifiedClasses: List<String>): Set<ClassId> =
        (Mocker.defaultSuperClassesToMockAlwaysNames + specifiedClasses).map { className ->
            ClassId(className)
        }.toSet()
}