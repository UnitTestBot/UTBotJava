package org.utbot.intellij.plugin.ui.utils

import org.utbot.framework.codegen.domain.TestFramework
import org.utbot.framework.plugin.api.MockFramework
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.LibraryOrderEntry
import org.utbot.framework.codegen.domain.DependencyInjectionFramework
import org.utbot.framework.plugin.api.utils.Patterns
import org.utbot.framework.plugin.api.utils.parametrizedTestsPatterns
import org.utbot.framework.plugin.api.utils.patterns
import org.utbot.framework.plugin.api.utils.testPatterns

fun findFrameworkLibrary(
    module: Module,
    testFramework: TestFramework,
    scope: LibrarySearchScope = LibrarySearchScope.Module,
): LibraryOrderEntry? = findMatchingLibraryOrNull(module, testFramework.patterns(), scope)

fun findFrameworkLibrary(
    module: Module,
    mockFramework: MockFramework,
    scope: LibrarySearchScope = LibrarySearchScope.Module,
): LibraryOrderEntry? = findMatchingLibraryOrNull(module, mockFramework.patterns(), scope)

fun findParametrizedTestsLibrary(
    module: Module,
    testFramework: TestFramework,
    scope: LibrarySearchScope = LibrarySearchScope.Module,
): LibraryOrderEntry? = findMatchingLibraryOrNull(module, testFramework.parametrizedTestsPatterns(), scope)

fun findDependencyInjectionLibrary(
    module: Module,
    springFrameworkType: DependencyInjectionFramework,
    scope: LibrarySearchScope = LibrarySearchScope.Module
): LibraryOrderEntry? = findMatchingLibraryOrNull(module, springFrameworkType.patterns(), scope)

fun findDependencyInjectionTestLibrary(
    module: Module,
    springFrameworkType: DependencyInjectionFramework,
    scope: LibrarySearchScope = LibrarySearchScope.Module
): LibraryOrderEntry? = findMatchingLibraryOrNull(module, springFrameworkType.testPatterns(), scope)

private fun findMatchingLibraryOrNull(
    module: Module,
    patterns: Patterns,
    scope: LibrarySearchScope,
): LibraryOrderEntry? {
    val installedLibraries = when (scope) {
        LibrarySearchScope.Module -> module.allLibraries()
        LibrarySearchScope.Project -> module.project.allLibraries()
    }

    return installedLibraries
        .matchesFrameworkPatterns(patterns.moduleLibraryPatterns, patterns.libraryPatterns)
}
