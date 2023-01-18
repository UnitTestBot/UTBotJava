package org.utbot.python.framework.api.python

import org.utbot.common.withToStringThreadLocalReentrancyGuard
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
    name: String  // includes module (like "_ast.Assign")
) : ClassId(name) {
    override fun toString(): String = name
    val rootModuleName: String = this.toString().split(".")[0]
    override val simpleName: String = name.split(".").last()
    val moduleName: String
        get() {
            return moduleOfType(name) ?: pythonBuiltinsModuleName
        }
    override val packageName = moduleName
    override val canonicalName = name
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
}

class PythonDefaultModel(
    val repr: String,
    classId: PythonClassId
): PythonModel(classId) {
    override fun toString() = repr
}

class PythonPrimitiveModel(
    val value: Any,
    classId: PythonClassId
): PythonModel(classId) {
    override fun toString() = "$value"
}

class PythonBoolModel(val value: Boolean): PythonModel(classId) {
    override fun toString() =
        if (value) "True" else "False"
    companion object {
        val classId = PythonClassId("builtins.bool")
    }
}

class PythonInitObjectModel(
    val type: String,
    val initValues: List<PythonModel>
): PythonModel(PythonClassId(type)) {
    override fun toString(): String {
        val params = initValues.joinToString(separator = ", ") { it.toString() }
        return "$type($params)"
    }

    override val allContainingClassIds: Set<PythonClassId>
        get() = super.allContainingClassIds + initValues.flatMap { it.allContainingClassIds }
}

class PythonListModel(
    val length: Int = 0,
    val stores: List<PythonModel>
) : PythonModel(classId) {
    override fun toString() =
        (0 until length).joinToString(", ", "[", "]") { stores[it].toString() }

    override val allContainingClassIds: Set<PythonClassId>
        get() = super.allContainingClassIds + stores.flatMap { it.allContainingClassIds }

    companion object {
        val classId = PythonClassId("builtins.list")
    }
}

class PythonTupleModel(
    val length: Int = 0,
    val stores: List<PythonModel>
) : PythonModel(classId) {
    override fun toString() =
        (0 until length).joinToString(", ", "(", ")") { stores[it].toString() }

    override val allContainingClassIds: Set<PythonClassId>
        get() = super.allContainingClassIds + stores.flatMap { it.allContainingClassIds }

    companion object {
        val classId = PythonClassId("builtins.tuple")
    }
}

class PythonDictModel(
    val length: Int = 0,
    val stores: Map<PythonModel, PythonModel>
) : PythonModel(classId) {
    override fun toString() = withToStringThreadLocalReentrancyGuard {
        stores.entries.joinToString(", ", "{", "}") { "${it.key}: ${it.value}" }
    }

    override val allContainingClassIds: Set<PythonClassId>
        get() = super.allContainingClassIds +
                stores.entries.flatMap { it.key.allContainingClassIds + it.value.allContainingClassIds }

    companion object {
        val classId = PythonClassId("builtins.dict")
    }
}

class PythonSetModel(
    val length: Int = 0,
    val stores: Set<PythonModel>
) : PythonModel(classId) {
    override fun toString() = withToStringThreadLocalReentrancyGuard {
        if (stores.isEmpty())
            "set()"
        else
            stores.joinToString(", ", "{", "}") { it.toString() }
    }

    override val allContainingClassIds: Set<PythonClassId>
        get() = super.allContainingClassIds + stores.flatMap { it.allContainingClassIds }

    companion object {
        val classId = PythonClassId("builtins.set")
    }
}
