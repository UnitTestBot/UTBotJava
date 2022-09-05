package org.utbot.framework.codegen.model.util

import org.utbot.framework.codegen.Junit4
import org.utbot.framework.codegen.Junit5
import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.TestNg
import org.utbot.framework.plugin.api.MockFramework

data class Patterns(
    val moduleLibraryPatterns: List<Regex>,
    val libraryPatterns: List<Regex>,
)

fun TestFramework.patterns(): Patterns {
    val moduleLibraryPatterns = when (this) {
        Junit4 -> junit4ModulePatterns
        Junit5 -> junit5ModulePatterns
        TestNg -> testNgModulePatterns
    }
    val libraryPatterns = when (this) {
        Junit4 -> junit4Patterns
        Junit5 -> junit5Patterns
        TestNg -> testNgPatterns
    }

    return Patterns(moduleLibraryPatterns, libraryPatterns)
}


fun TestFramework.parametrizedTestsPatterns(): Patterns {
    val moduleLibraryPatterns = when (this) {
        Junit4 -> emptyList()
        Junit5 -> emptyList()   // emptyList here because JUnit5 module may not be enough for parametrized tests if :junit-jupiter-params: is not installed
        TestNg -> testNgModulePatterns
    }
    val libraryPatterns = when (this) {
        Junit4 -> emptyList()
        Junit5 -> junit5ParametrizedTestsPatterns
        TestNg -> testNgPatterns
    }

    return Patterns(moduleLibraryPatterns, libraryPatterns)
}


fun MockFramework.patterns(): Patterns {
    val moduleLibraryPatterns = when (this) {
        MockFramework.MOCKITO -> mockitoModulePatterns
    }
    val libraryPatterns = when (this) {
        MockFramework.MOCKITO -> mockitoPatterns
    }

    return Patterns(moduleLibraryPatterns, libraryPatterns)
}

val JUNIT_4_JAR_PATTERN = Regex("junit-4(\\.1[2-9])(\\.[0-9]+)?")
val JUNIT_4_MVN_PATTERN = Regex("junit:junit:4(\\.1[2-9])(\\.[0-9]+)?")
val junit4Patterns = listOf(JUNIT_4_JAR_PATTERN, JUNIT_4_MVN_PATTERN)
val junit4ModulePatterns = listOf(JUNIT_4_JAR_PATTERN, JUNIT_4_MVN_PATTERN)

val JUNIT_5_JAR_PATTERN = Regex("junit-jupiter-5(\\.[0-9]+){1,2}")
val JUNIT_5_MVN_PATTERN = Regex("org\\.junit\\.jupiter:junit-jupiter-api:5(\\.[0-9]+){1,2}")
val JUNIT_5_BASIC_PATTERN = Regex("JUnit5\\.4")
val junit5Patterns = listOf(JUNIT_5_JAR_PATTERN, JUNIT_5_MVN_PATTERN, JUNIT_5_BASIC_PATTERN)

val JUNIT_5_PARAMETRIZED_JAR_PATTERN = Regex("junit-jupiter-params-5(\\.[0-9]+){1,2}")
val JUNIT_5_PARAMETRIZED_MVN_PATTERN = Regex("org\\.junit\\.jupiter\\.junit-jupiter-params:5(\\.[0-9]+){1,2}")
val junit5ParametrizedTestsPatterns = listOf(JUNIT_5_JAR_PATTERN, JUNIT_5_BASIC_PATTERN,
    JUNIT_5_PARAMETRIZED_JAR_PATTERN, JUNIT_5_PARAMETRIZED_MVN_PATTERN)

val JUNIT5_BASIC_MODULE_PATTERN = Regex("junit-jupiter")
val junit5ModulePatterns = listOf(JUNIT5_BASIC_MODULE_PATTERN)

val TEST_NG_JAR_PATTERN = Regex("testng-[0-9](\\.[0-9]+){2}")
val TEST_NG_MVN_PATTERN = Regex("org\\.testng:testng:[0-9](\\.[0-9]+){2}")
val TEST_NG_BASIC_PATTERN = Regex("testng")
val testNgPatterns = listOf(TEST_NG_JAR_PATTERN, TEST_NG_MVN_PATTERN, TEST_NG_BASIC_PATTERN)

val TEST_NG_BASIC_MODULE_PATTERN = Regex("testng")
val testNgModulePatterns = listOf(TEST_NG_BASIC_MODULE_PATTERN)

val MOCKITO_JAR_PATTERN = Regex("mockito-core-[3-9](\\.[0-9]+){2}")
val MOCKITO_MVN_PATTERN = Regex("org\\.mockito:mockito-core:[3-9](\\.[0-9]+){2}")
val mockitoPatterns = listOf(MOCKITO_JAR_PATTERN, MOCKITO_MVN_PATTERN)

val MOCKITO_BASIC_MODULE_PATTERN = Regex("mockito-core")
val mockitoModulePatterns = listOf(MOCKITO_BASIC_MODULE_PATTERN)

const val MOCKITO_EXTENSIONS_STORAGE = "mockito-extensions"
const val MOCKITO_MOCKMAKER_FILE_NAME = "org.mockito.plugins.MockMaker"
val MOCKITO_EXTENSIONS_FILE_CONTENT = listOf("mock-maker-inline")