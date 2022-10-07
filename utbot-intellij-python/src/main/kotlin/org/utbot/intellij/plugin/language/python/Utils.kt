package org.utbot.intellij.plugin.language.python

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

inline fun <reified T: PsiElement> getContainingElement(
    element: PsiElement,
    predicate: (T) -> Boolean = { true }
): T? {
    var result = element
    while ((result !is T || !predicate(result)) && (result.parent != null)) {
        result = result.parent
    }
    return result as? T
}

fun getContentRoot(project: Project, file: VirtualFile): VirtualFile {
    return ProjectFileIndex.getInstance(project)
        .getContentRootForFile(file) ?: error("Source file lies outside of a module")
}