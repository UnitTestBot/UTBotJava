package org.utbot.framework.plugin.api.util

data class Patterns(
    val moduleLibraryPatterns: List<Regex>,
    val libraryPatterns: List<Regex>,
)

val codegenUtilsLibraryPatterns: Patterns
    get() = Patterns(
        moduleLibraryPatterns = listOf(CODEGEN_UTILS_JAR_PATTERN),
        libraryPatterns = emptyList()
    )

private val CODEGEN_UTILS_JAR_PATTERN = Regex("utbot-codegen-utils-[0-9](\\.[0-9]+){2}")
