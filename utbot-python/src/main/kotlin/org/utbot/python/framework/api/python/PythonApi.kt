package org.utbot.python.framework.api.python

import org.utbot.framework.plugin.api.*
import org.utbot.python.PythonArgument
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
    val prettyName: String = if (rootModuleName == pythonBuiltinsModuleName)
        name.split(".", limit=2).last()
    else
        name
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
        if (other !is PythonTreeModel) {
            return false
        }
        return tree == other.tree
    }

    override fun hashCode(): Int {
        return tree.hashCode()
    }
}

class PythonUtExecution(
    val stateInit: EnvironmentModels,
    stateBefore: EnvironmentModels,
    stateAfter: EnvironmentModels,
    val diffIds: List<Long>,
    result: UtExecutionResult,
    val arguments: List<PythonArgument>,
    coverage: Coverage? = null,
    summary: List<DocStatement>? = null,
    testMethodName: String? = null,
    displayName: String? = null,
) : UtExecution(stateBefore, stateAfter, result, coverage, summary, testMethodName, displayName) {
    init {
        stateInit.parameters.zip(stateBefore.parameters).map { (init, before) ->
            if (init is PythonTreeModel && before is PythonTreeModel) {
                init.tree.comparable = before.tree.comparable
            }
        }
        val init = stateInit.thisInstance
        val before = stateBefore.thisInstance
        if (init is PythonTreeModel && before is PythonTreeModel) {
            init.tree.comparable = before.tree.comparable
        }
    }
    override fun copy(
        stateBefore: EnvironmentModels,
        stateAfter: EnvironmentModels,
        result: UtExecutionResult,
        coverage: Coverage?,
        summary: List<DocStatement>?,
        testMethodName: String?,
        displayName: String?
    ): UtExecution {
        return PythonUtExecution(
            stateInit = stateInit,
            stateBefore = stateBefore,
            stateAfter = stateAfter,
            diffIds = diffIds,
            result = result,
            coverage = coverage,
            summary = summary,
            testMethodName = testMethodName,
            displayName = displayName,
            arguments = arguments
        )
    }
}