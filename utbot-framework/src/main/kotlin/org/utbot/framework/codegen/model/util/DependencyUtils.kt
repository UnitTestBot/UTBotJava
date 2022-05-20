package org.utbot.framework.codegen.model.util

import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.concrete.UtExecutionInstrumentation
import org.utbot.framework.plugin.api.MockFramework
import java.io.File
import java.util.jar.JarFile
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Checks that dependency paths contains some frameworks
 * and their versions correspond to our requirements.
 *
 * Note: [UtExecutionInstrumentation] must be in dependency path too
 * as it is used by Engine in the child process in Concrete Executor.
 */
fun checkFrameworkDependencies(dependencyPaths: String?) {
    if (dependencyPaths.isNullOrEmpty()) {
        error("Dependency paths is empty, no test framework and mock framework to generate tests")
    }

    //TODO: JIRA:1659
    // check (somehow) that [UtExecutionInstrumentation] is in dependency path: in one of jars or folders

    val dependencyNames = dependencyPaths
        .splitToSequence(File.pathSeparatorChar)
        .mapNotNull { findDependencyName(it) }
        .map { it.toLowerCase() }
        .toSet()

    val testFrameworkPatterns = TestFramework.allItems.map { it.patterns() }
    val testFrameworkFound = dependencyNames.matchesAnyOf(testFrameworkPatterns)
    if (!testFrameworkFound) {
        error("""
          Test frameworks are not found in dependency path $dependencyPaths, dependency names are:
          ${dependencyNames.joinToString(System.lineSeparator())}
          """
        )
    }

    val mockFrameworkPatterns = MockFramework.allItems.map { it.patterns() }
    val mockFrameworkFound = dependencyNames.matchesAnyOf(mockFrameworkPatterns)
    if (!mockFrameworkFound) {
        error("""
          Mock frameworks are not found in dependency path $dependencyPaths, dependency names are:
          ${dependencyNames.joinToString(System.lineSeparator())}
          """
        )
    }
}

private fun Set<String>.matchesAnyOf(patterns: List<Patterns>): Boolean {
    val expressions = patterns.flatMap { it.moduleLibraryPatterns + it.libraryPatterns }
    return any { libraryName ->
        expressions.any { expr -> libraryName.let { expr.containsMatchIn(it) } }
    }
}

private fun findDependencyName(jarPath: String): String? {
    try {
        val attributes = JarFile(jarPath).manifest.mainAttributes

        val bundleName = attributes.getValue("Bundle-SymbolicName")
        val bundleVersion = attributes.getValue("Bundle-Version")
        val moduleName = attributes.getValue("Automatic-Module-Name")
        val implementationTitle = attributes.getValue("Implementation-Title")
        val implementationVersion = attributes.getValue("Implementation-Version")

        if (bundleName != null) return "$bundleName:$bundleVersion"
        if (moduleName != null) return "$moduleName:$implementationTitle:$implementationVersion"
    } catch (e: Exception) {
        logger.warn { "Unexpected error during parsing $jarPath manifest file $e" }
    }

    return null
}