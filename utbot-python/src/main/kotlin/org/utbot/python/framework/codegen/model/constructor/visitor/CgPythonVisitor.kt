package org.utbot.python.framework.codegen.model.constructor.visitor

import org.utbot.framework.codegen.renderer.CgVisitor
import org.utbot.python.framework.codegen.model.tree.*

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