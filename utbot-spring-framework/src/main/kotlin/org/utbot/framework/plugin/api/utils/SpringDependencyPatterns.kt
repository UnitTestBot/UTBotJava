package org.utbot.framework.plugin.api.utils

import org.utbot.framework.codegen.domain.SpringModule
import org.utbot.framework.codegen.domain.SpringModule.*

fun SpringModule.patterns(): Patterns {
    val moduleLibraryPatterns = when (this) {
        SPRING_BOOT -> springBootModulePatterns
        SPRING_BEANS -> springBeansModulePatterns
        SPRING_SECURITY -> springSecurityModulePatterns
    }
    val libraryPatterns = when (this) {
        SPRING_BOOT -> springBootPatterns
        SPRING_BEANS -> springBeansPatterns
        SPRING_SECURITY -> springSecurityPatterns
    }

    return Patterns(moduleLibraryPatterns, libraryPatterns)
}

fun SpringModule.testPatterns(): Patterns {
    val moduleLibraryPatterns = when (this) {
        SPRING_BOOT -> springBootTestModulePatterns
        SPRING_BEANS -> springBeansTestModulePatterns
        SPRING_SECURITY -> springSecurityTestModulePatterns
    }
    val libraryPatterns = when (this) {
        SPRING_BOOT -> springBootTestPatterns
        SPRING_BEANS -> springBeansTestPatterns
        SPRING_SECURITY -> springSecurityTestPatterns
    }

    return Patterns(moduleLibraryPatterns, libraryPatterns)
}

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
val SPRING_BOOT_MVN_PATTERN = Regex("org\\.springframework\\.boot:spring-boot:([0-9]+)(\\.[0-9]+){1,2}")
val springBootPatterns = listOf(SPRING_BOOT_JAR_PATTERN, SPRING_BOOT_MVN_PATTERN)

val SPRING_BOOT_BASIC_MODULE_PATTERN = Regex("spring-boot")
val springBootModulePatterns = listOf(SPRING_BOOT_BASIC_MODULE_PATTERN)

val SPRING_BOOT_TEST_JAR_PATTERN = Regex("spring-boot-test-([0-9]+)(\\.[0-9]+){1,2}")
val SPRING_BOOT_TEST_MVN_PATTERN = Regex("org\\.springframework\\.boot:spring-boot-test:([0-9]+)(\\.[0-9]+){1,2}")

val springBootTestPatterns = listOf(SPRING_BOOT_TEST_JAR_PATTERN, SPRING_BOOT_TEST_MVN_PATTERN)

val SPRING_BOOT_TEST_BASIC_MODULE_PATTERN = Regex("spring-boot-test")
val springBootTestModulePatterns = listOf(SPRING_BOOT_TEST_BASIC_MODULE_PATTERN)

val SPRING_SECURITY_JAR_PATTERN = Regex("spring-security-core-([0-9]+)(\\.[0-9]+){1,2}")
val SPRING_SECURITY_MVN_PATTERN = Regex("org\\.springframework\\.security:spring-security-core:([0-9]+)(\\.[0-9]+){1,2}")
val springSecurityPatterns = listOf(SPRING_SECURITY_JAR_PATTERN, SPRING_SECURITY_MVN_PATTERN)

val SPRING_SECURITY_BASIC_MODULE_PATTERN = Regex("spring-security-core")
val springSecurityModulePatterns = listOf(SPRING_SECURITY_BASIC_MODULE_PATTERN)

val SPRING_SECURITY_TEST_JAR_PATTERN = Regex("spring-security-test-([0-9]+)(\\.[0-9]+){1,2}")
val SPRING_SECURITY_TEST_MVN_PATTERN = Regex("org\\.springframework\\.security:spring-security-test:([0-9]+)(\\.[0-9]+){1,2}")
val springSecurityTestPatterns = listOf(SPRING_SECURITY_TEST_JAR_PATTERN, SPRING_SECURITY_TEST_MVN_PATTERN)

val SPRING_SECURITY_TEST_BASIC_MODULE_PATTERN = Regex("spring-security-test")
val springSecurityTestModulePatterns = listOf(SPRING_SECURITY_TEST_BASIC_MODULE_PATTERN)