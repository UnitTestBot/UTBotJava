package org.utbot.python.framework.codegen.model.tree

import org.utbot.framework.codegen.domain.models.CgElement
import org.utbot.framework.codegen.domain.models.CgExpression
import org.utbot.framework.codegen.domain.models.CgLiteral
import org.utbot.framework.codegen.domain.models.CgStatement
import org.utbot.framework.codegen.domain.models.CgValue
import org.utbot.framework.codegen.domain.models.CgVariable
import org.utbot.framework.codegen.renderer.CgVisitor
import org.utbot.framework.plugin.api.ClassId
import org.utbot.python.framework.api.python.PythonClassId
import org.utbot.python.framework.api.python.PythonTree
import org.utbot.python.framework.api.python.util.*
import org.utbot.python.framework.codegen.model.constructor.visitor.CgPythonVisitor

interface CgPythonElement : CgElement {
    override fun <R> accept(visitor: CgVisitor<R>): R = visitor.run {
        if (visitor is CgPythonVisitor<R>) {
            when (val element = this@CgPythonElement) {
                is CgPythonRepr -> visitor.visit(element)
                is CgPythonIndex -> visitor.visit(element)
                is CgPythonAssertEquals -> visitor.visit(element)
                is CgPythonFunctionCall -> visitor.visit(element)
                is CgPythonRange -> visitor.visit(element)
                is CgPythonList -> visitor.visit(element)
                is CgPythonSet -> visitor.visit(element)
                is CgPythonDict -> visitor.visit(element)
                is CgPythonTuple -> visitor.visit(element)
                is CgPythonTree -> visitor.visit(element)
                else -> throw IllegalArgumentException("Can not visit element of type ${element::class}")
            }
        } else {
            super.accept(visitor)
        }
    }
}

class CgPythonTree(
    override val type: ClassId,
    val tree: PythonTree.PythonTreeNode
) : CgValue, CgPythonElement {
    fun getChildren(): List<CgPythonTree> {
        return tree.children.map { CgPythonTree(it.type, it) }
    }
    fun getDictChildren(): Map<CgPythonTree, CgPythonTree> {
        if (tree is PythonTree.DictNode) {
            return tree.items.map { CgPythonTree(it.key.type, it.key) to CgPythonTree(it.value.type, it.value) }.toMap()
        } else {
            throw IllegalArgumentException("$tree is not a dict")
        }

    }
}

class CgPythonRepr(
    override val type: ClassId,
    val content: String
) : CgValue, CgPythonElement

class CgPythonAssertEquals(
    val expression: CgExpression,
    val keyword: String = "assert",
) : CgStatement, CgPythonElement

class CgPythonFunctionCall(
    override val type: PythonClassId,
    val name: String,
    val parameters: List<CgExpression>,
) : CgExpression, CgPythonElement

class CgPythonIndex(
    override val type: PythonClassId,
    val obj: CgVariable,
    val index: CgExpression,
) : CgValue, CgPythonElement

class CgPythonRange(
    val start: CgValue,
    val stop: CgValue,
    val step: CgValue,
) : CgValue, CgPythonElement {
    override val type: PythonClassId
        get() = pythonRangeClassId

    constructor(stop: Int) : this(
        CgLiteral(pythonIntClassId, 0),
        CgLiteral(pythonIntClassId, stop),
        CgLiteral(pythonIntClassId, 1),
    )

    constructor(stop: CgValue) : this(
        CgLiteral(pythonIntClassId, 0),
        stop,
        CgLiteral(pythonIntClassId, 1),
    )
}

class CgPythonList(
    val elements: List<CgValue>
) : CgValue, CgPythonElement {
    override val type: PythonClassId = pythonListClassId
}

class CgPythonTuple(
    val elements: List<CgValue>
) : CgValue, CgPythonElement {
    override val type: PythonClassId = pythonTupleClassId
}

class CgPythonSet(
    val elements: Set<CgValue>
) : CgValue, CgPythonElement {
    override val type: PythonClassId = pythonSetClassId
}

class CgPythonDict(
    val elements: Map<CgValue, CgValue>
) : CgValue, CgPythonElement {
    override val type: PythonClassId = pythonDictClassId
}