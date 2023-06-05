package org.utbot.framework.plugin.api.utils

import org.utbot.framework.codegen.domain.DependencyInjectionFramework
import org.utbot.framework.codegen.domain.Junit4
import org.utbot.framework.codegen.domain.Junit5
import org.utbot.framework.codegen.domain.SpringBeans
import org.utbot.framework.codegen.domain.SpringBoot
import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.codegen.domain.TestNg
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
        else -> throw UnsupportedOperationException("Unknown test framework $this")
    }
    val libraryPatterns = when (this) {
        Junit4 -> junit4Patterns
        Junit5 -> junit5Patterns
        TestNg -> testNgPatterns
        else -> throw UnsupportedOperationException("Unknown test framework $this")
    }

    return Patterns(moduleLibraryPatterns, libraryPatterns)
}


fun TestFramework.parametrizedTestsPatterns(): Patterns {
    val moduleLibraryPatterns = when (this) {
        Junit4 -> emptyList()
        Junit5 -> emptyList()   // emptyList here because JUnit5 module may not be enough for parametrized tests if :junit-jupiter-params: is not installed
        TestNg -> testNgModulePatterns
        else -> throw UnsupportedOperationException("Unknown test framework $this")
    }
    val libraryPatterns = when (this) {
        Junit4 -> emptyList()
        Junit5 -> junit5ParametrizedTestsPatterns
        TestNg -> testNgPatterns
        else -> throw UnsupportedOperationException("Unknown test framework $this")
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

fun DependencyInjectionFramework.patterns(): Patterns {
    val moduleLibraryPatterns = when (this) {
        SpringBoot -> springBootModulePatterns
        SpringBeans -> springBeansModulePatterns
        else -> throw UnsupportedOperationException("Unknown dependency injection framework $this")
    }
    val libraryPatterns = when (this) {
        SpringBoot -> springBootPatterns
        SpringBeans -> springBeansPatterns
        else -> throw UnsupportedOperationException("Unknown dependency injection framework $this")
    }

    return Patterns(moduleLibraryPatterns, libraryPatterns)
}

fun DependencyInjectionFramework.testPatterns(): Patterns {
    val moduleLibraryPatterns = when (this) {
        SpringBoot -> springBootTestModulePatterns
        SpringBeans -> springBeansTestModulePatterns
        else -> throw UnsupportedOperationException("Unknown dependency injection framework $this")
    }
    val libraryPatterns = when (this) {
        SpringBoot -> springBootTestPatterns
        SpringBeans -> springBeansTestPatterns
        else -> throw UnsupportedOperationException("Unknown dependency injection framework $this")
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

const val MOCKITO_EXTENSIONS_FOLDER = "mockito-extensions"
const val MOCKITO_MOCKMAKER_FILE_NAME = "org.mockito.plugins.MockMaker"
val MOCKITO_EXTENSIONS_FILE_CONTENT = "mock-maker-inline"

val SPRING_BEANS_JAR_PATTERN = Regex("spring-beans-([0-9]+)(\\.[0-9]+){1,2}")
val SPRING_BEANS_MVN_PATTERN = Regex("org\\.springframework:spring-beans:([0-9]+)(\\.[0-9]+){1,2}")
val springBeansPatterns = listOf(SPRING_BEANS_JAR_PATTERN, SPRING_BEANS_MVN_PATTERN)

val SPRING_BEANS_BASIC_MODULE_PATTERN = Regex("spring-beans")
val springBeansModulePatterns = listOf(SPRING_BEANS_BASIC_MODULE_PATTERN)

val SPRING_BEANS_TEST_JAR_PATTERN = Regex("spring-test-([0-9]+)(\\.[0-9]+){1,2}")
val SPRING_BEANS_TEST_MVN_PATTERN = Regex("org\\.springframework:spring-test:([0-9]+)(\\.[0-9]+){1,2}")
val springBeansTestPatterns = listOf(SPRING_BEANS_TEST_JAR_PATTERN, SPRING_BEANS_TEST_MVN_PATTERN)

val SPRING_BEANS_TEST_BASIC_MODULE_PATTERN = Regex("spring-test")
val springBeansTestModulePatterns = listOf(SPRING_BEANS_TEST_BASIC_MODULE_PATTERN)

val SPRING_BOOT_JAR_PATTERN = Regex("spring-boot-([0-9]+)(\\.[0-9]+){1,2}")
val SPRING_BOOT_STARTER_JAR_PATTERN = Regex("spring-boot-starter*-([0-9]+)(\\.[0-9]+){1,2}")
val SPRING_BOOT_MVN_PATTERN = Regex("org\\.springframework\\.boot:spring-boot:([0-9]+)(\\.[0-9]+){1,2}")
val SPRING_BOOT_STARTER_MVN_PATTERN = Regex("org\\.springframework\\.boot:spring-boot-starter*:([0-9]+)(\\.[0-9]+){1,2}")
val springBootPatterns = listOf(
    SPRING_BOOT_JAR_PATTERN,
    SPRING_BOOT_STARTER_JAR_PATTERN,
    SPRING_BOOT_MVN_PATTERN,
    SPRING_BOOT_STARTER_MVN_PATTERN,
    )

val SPRING_BOOT_BASIC_MODULE_PATTERN = Regex("spring-boot")
val SPRING_BOOT_STARTER_BASIC_MODULE_PATTERN = Regex("spring-boot-starter")
val springBootModulePatterns = listOf(SPRING_BOOT_BASIC_MODULE_PATTERN, SPRING_BOOT_STARTER_BASIC_MODULE_PATTERN)

val SPRING_BOOT_TEST_JAR_PATTERN = Regex("spring-boot-test-([0-9]+)(\\.[0-9]+){1,2}")
val SPRING_BOOT_TEST_MVN_PATTERN = Regex("org\\.springframework\\.boot:spring-boot-test:([0-9]+)(\\.[0-9]+){1,2}")

val springBootTestPatterns = listOf(SPRING_BOOT_TEST_JAR_PATTERN, SPRING_BOOT_TEST_MVN_PATTERN)

val SPRING_BOOT_TEST_BASIC_MODULE_PATTERN = Regex("spring-boot-test")
val springBootTestModulePatterns = listOf(SPRING_BOOT_TEST_BASIC_MODULE_PATTERN)
