package org.utbot.python.framework.api.python

import org.utbot.framework.plugin.api.ClassId
import org.utbot.framework.plugin.api.MethodId
import org.utbot.framework.plugin.api.UtModel
import org.utbot.python.framework.api.python.util.moduleOfType

/**
 * PythonClassId represents Python type.
 * NormalizedPythonAnnotation represents annotation after normalization.
 *
 * Example of PythonClassId, but not NormalizedPythonAnnotation:
 *  builtins.list (normalized annotation is typing.List[typing.Any])
 */

const val pythonBuiltinsModuleName = "builtins"

class PythonClassId(
    val moduleName: String,
    val typeName: String,
) : ClassId("$moduleName.$typeName") {
    constructor(fullName: String) : this(
        moduleOfType(fullName) ?: pythonBuiltinsModuleName,
        fullName.removePrefix(moduleOfType(fullName) ?: pythonBuiltinsModuleName).removePrefix(".")
    )
    override fun toString(): String = canonicalName
    val rootModuleName: String = moduleName.split(".").first()
    override val simpleName: String = typeName
    override val canonicalName = name
    override val packageName = moduleName
}

open class RawPythonAnnotation(
    annotation: String
): ClassId(annotation)

class NormalizedPythonAnnotation(
    annotation: String
) : RawPythonAnnotation(annotation)

class PythonMethodId(
    override val classId: PythonClassId,  // may be a fake class for top-level functions
    override val name: String,
    override val returnType: RawPythonAnnotation,
    override val parameters: List<RawPythonAnnotation>,
) : MethodId(classId, name, returnType, parameters) {
    val moduleName: String = classId.moduleName
    val rootModuleName: String = this.toString().split(".")[0]
    override fun toString(): String = if (moduleName.isNotEmpty()) "$moduleName.$name" else name
}

sealed class PythonModel(classId: PythonClassId): UtModel(classId) {
    open val allContainingClassIds: Set<PythonClassId> = setOf(classId)
}

class PythonTreeModel(
    val tree: PythonTree.PythonTreeNode,
    classId: PythonClassId,
): PythonModel(classId) {
    constructor(tree: PythonTree.PythonTreeNode) : this(tree, tree.type)

    override val allContainingClassIds: Set<PythonClassId>
        get() { return findAllContainingClassIds(setOf(this.tree)) }

    private fun findAllContainingClassIds(visited: Set<PythonTree.PythonTreeNode>): Set<PythonClassId> {
        val children = tree.children.map { PythonTreeModel(it, it.type) }
        val newVisited = (visited + setOf(this.tree)).toMutableSet()
        val childrenClassIds = children.filterNot { newVisited.contains(it.tree) }.flatMap {
            newVisited.add(it.tree)
            it.findAllContainingClassIds(newVisited)
        }
        return super.allContainingClassIds + childrenClassIds
    }

    override fun equals(other: Any?): Boolean {
        if (other is PythonTreeModel) {
            return tree.softEquals(other.tree)
        }
        return false
    }

    override fun hashCode(): Int {
        return tree.hashCode()
    }
}
