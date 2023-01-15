package org.utbot.intellij.plugin.language.python

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyFunction
import org.utbot.python.PythonMethodDescription
import org.utbot.python.framework.api.python.PythonClassId
import kotlin.random.Random

inline fun <reified T : PsiElement> getContainingElement(
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

fun generateRandomString(length: Int): String {
    val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    return (0..length)
        .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
        .joinToString("")
}

fun PyFunction.toPythonMethodDescription(): PythonMethodDescription? {
    return PythonMethodDescription(
        this.name ?: return null,
        // this.parameterList.parameters.mapNotNull { it.name?.let { arg -> PythonArgument(arg, "") } },
        this.containingFile.virtualFile?.canonicalPath ?: "",
        this.containingClass?.name?.let{ PythonClassId(it) },
        //""
    )
}
