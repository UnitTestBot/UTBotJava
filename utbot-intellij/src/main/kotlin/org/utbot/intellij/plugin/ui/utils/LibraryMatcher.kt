package org.utbot.intellij.plugin.ui.utils

import org.utbot.framework.codegen.TestFramework
import org.utbot.framework.codegen.model.util.patterns
import org.utbot.framework.plugin.api.MockFramework
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import org.utbot.framework.plugin.api.util.Patterns
import org.utbot.framework.plugin.api.util.codegenUtilsLibraryPatterns

fun findFrameworkLibrary(
    project: Project,
    testModule: Module,
    testFramework: TestFramework,
    scope: LibrarySearchScope = LibrarySearchScope.Module,
): LibraryOrderEntry? {
    return findMatchingLibrary(project, testModule, testFramework.patterns(), scope)
}

fun findFrameworkLibrary(
    project: Project,
    testModule: Module,
    mockFramework: MockFramework,
    scope: LibrarySearchScope = LibrarySearchScope.Module,
): LibraryOrderEntry? = findMatchingLibrary(project, testModule, mockFramework.patterns(), scope)

fun findCodegenUtilsLibrary(
    project: Project,
    testModule: Module,
    scope: LibrarySearchScope = LibrarySearchScope.Module,
): LibraryOrderEntry? {
    return findMatchingLibrary(project, testModule, codegenUtilsLibraryPatterns, scope)
}

private fun findMatchingLibrary(
    project: Project,
    testModule: Module,
    patterns: Patterns,
    scope: LibrarySearchScope,
): LibraryOrderEntry? {
    val installedLibraries = when (scope) {
        LibrarySearchScope.Module -> testModule.allLibraries()
        LibrarySearchScope.Project -> project.allLibraries()
    }
    return installedLibraries
        .matchesFrameworkPatterns(patterns.moduleLibraryPatterns, patterns.libraryPatterns)
}
