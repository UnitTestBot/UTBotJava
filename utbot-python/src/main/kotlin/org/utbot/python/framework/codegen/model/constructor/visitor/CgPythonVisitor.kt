package org.utbot.python.framework.codegen.model.constructor.visitor

import org.utbot.framework.codegen.renderer.CgVisitor
import org.utbot.python.framework.codegen.model.tree.CgPythonAssertEquals
import org.utbot.python.framework.codegen.model.tree.CgPythonDict
import org.utbot.python.framework.codegen.model.tree.CgPythonFunctionCall
import org.utbot.python.framework.codegen.model.tree.CgPythonIndex
import org.utbot.python.framework.codegen.model.tree.CgPythonIterator
import org.utbot.python.framework.codegen.model.tree.CgPythonList
import org.utbot.python.framework.codegen.model.tree.CgPythonNamedArgument
import org.utbot.python.framework.codegen.model.tree.CgPythonRange
import org.utbot.python.framework.codegen.model.tree.CgPythonRepr
import org.utbot.python.framework.codegen.model.tree.CgPythonSet
import org.utbot.python.framework.codegen.model.tree.CgPythonTree
import org.utbot.python.framework.codegen.model.tree.CgPythonTuple
import org.utbot.python.framework.codegen.model.tree.CgPythonWith
import org.utbot.python.framework.codegen.model.tree.CgPythonZip

interface CgPythonVisitor<R> : CgVisitor<R> {

    fun visit(element: CgPythonRepr): R
    fun visit(element: CgPythonIndex): R
    fun visit(element: CgPythonAssertEquals): R
    fun visit(element: CgPythonFunctionCall): R
    fun visit(element: CgPythonRange): R
    fun visit(element: CgPythonDict): R
    fun visit(element: CgPythonTuple): R
    fun visit(element: CgPythonList): R
    fun visit(element: CgPythonSet): R
    fun visit(element: CgPythonIterator): R
    fun visit(element: CgPythonTree): R
    fun visit(element: CgPythonWith): R
    fun visit(element: CgPythonNamedArgument): R
    fun visit(element: CgPythonZip): R
}