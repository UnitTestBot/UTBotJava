package org.utbot.intellij.plugin.python

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFunction
import org.utbot.intellij.plugin.python.table.UtPyClassItem
import org.utbot.intellij.plugin.python.table.UtPyFunctionItem
import org.utbot.intellij.plugin.python.table.UtPyTableItem
import org.utbot.python.utils.RequirementsUtils
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

fun getAncestors(element: PsiElement): List<PsiElement> =
    if (element.parent == null)
        listOf(element)
    else
        getAncestors(element.parent) + element

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

fun VirtualFile.isProjectSubmodule(ancestor: VirtualFile?): Boolean {
    return VfsUtil.isUnder(this, setOf(ancestor).toMutableSet())
}

fun checkModuleIsInstalled(pythonPath: String, moduleName: String): Boolean {
    return RequirementsUtils.requirementsAreInstalled(pythonPath, listOf(moduleName))
}

fun fineFunction(function: PyFunction): Boolean {
    val hasNotConstructorName = !listOf("__init__", "__new__").contains(function.name)
    val decoratorNames = function.decoratorList?.decorators?.mapNotNull { it?.qualifiedName }
    val knownDecorators = decoratorNames?.all { it.toString() in listOf("staticmethod") } ?: true
    return hasNotConstructorName && knownDecorators
}

fun fineClass(pyClass: PyClass): Boolean =
    getAncestors(pyClass).dropLast(1).all { it !is PyClass && it !is PyFunction } &&
            pyClass.methods.any { fineFunction(it) }

fun PsiDirectory.topParent(level: Int): PsiDirectory? {
    var directory: PsiDirectory? = this
    repeat(level) {
        directory = directory?.parent
    }
    return directory
}

fun PyElement.fileName(): String? = this.containingFile.virtualFile.canonicalPath

fun PyElement.toUtPyTableItem(): UtPyTableItem? {
    return when (this) {
        is PyClass -> UtPyClassItem(this)
        is PyFunction -> UtPyFunctionItem(this)
        else -> null
    }
}