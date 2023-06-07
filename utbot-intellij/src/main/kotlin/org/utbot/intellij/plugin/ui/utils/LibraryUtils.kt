package org.utbot.intellij.plugin.ui.utils

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderEnumerator
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager

/**
 * Defines the scope to search a library.
 */
enum class LibrarySearchScope {
    Module,
    Project,
}

fun Project.allLibraries(): List<LibraryOrderEntry> {
    val projectRootManager = ProjectRootManager.getInstance(this)
    return allLibraries(projectRootManager.orderEntries())
}

fun Module.allLibraries(): List<LibraryOrderEntry> {
    val moduleRootManager = ModuleRootManager.getInstance(this)
    return allLibraries(moduleRootManager.orderEntries())
}
fun List<LibraryOrderEntry>.matchesAnyOf(patterns: List<Regex>): LibraryOrderEntry? =
    firstOrNull { entry ->
        patterns.any { pattern ->
            entry.libraryName?.let {
                if (pattern.containsMatchIn(it)) return@any true
            }
            //Fallback to filenames in case library has no name at all, or the name is too generic (e.g. 'JUnit' or 'JUnit4')
            return@any entry.library?.getFiles(OrderRootType.CLASSES)
                ?.any { virtualFile -> pattern.containsMatchIn(virtualFile.name) } ?: false
        }
    }

fun List<LibraryOrderEntry>.matchesFrameworkPatterns(
    moduleLibraryPatterns: List<Regex>,
    libraryPatterns: List<Regex>,
): LibraryOrderEntry? {
    val moduleLibrary = matchesAnyOf(moduleLibraryPatterns)
    if (moduleLibrary != null) {
        return moduleLibrary
    }

    return matchesAnyOf(libraryPatterns)
}

private fun allLibraries(orderEntries: OrderEnumerator): List<LibraryOrderEntry> {
    val libraries = mutableListOf<LibraryOrderEntry>()
    orderEntries.forEach {
        if (it is LibraryOrderEntry) {
            libraries += it
        }
        true
    }

    return libraries
}